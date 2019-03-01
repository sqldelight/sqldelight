package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.FileSpec
import com.squareup.sqldelight.core.compiler.QueryInterfaceGenerator
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.compiler.TableInterfaceGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class InterfaceGeneration {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun noUniqueQueries() {
    checkFixtureCompiles("no-unique-queries")
  }

  @Test fun queryRequiresType() {
    checkFixtureCompiles("query-requires-type")
  }

  @Test fun `left joins apply nullability to columns`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  val1 TEXT NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  val2 TEXT NOT NULL
      |);
      |
      |leftJoin:
      |SELECT *
      |FROM A LEFT OUTER JOIN B;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface LeftJoin {
      |    val val1: kotlin.String
      |
      |    val val2: kotlin.String?
      |
      |    data class Impl(override val val1: kotlin.String, override val val2: kotlin.String?) : com.example.LeftJoin {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |LeftJoin.Impl [
      |        |  val1: ${"$"}val1
      |        |  val2: ${"$"}val2
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `duplicated column name uses table prefix`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  value TEXT NOT NULL
      |);
      |
      |leftJoin:
      |SELECT *
      |FROM A JOIN B;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface LeftJoin {
      |    val value: kotlin.String
      |
      |    val value_: kotlin.String
      |
      |    data class Impl(override val value: kotlin.String, override val value_: kotlin.String) : com.example.LeftJoin {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |LeftJoin.Impl [
      |        |  value: ${"$"}value
      |        |  value_: ${"$"}value_
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `incompatible adapter types revert to sqlite types`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |CREATE TABLE B(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, value
      |FROM B;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.String?
      |
      |    val value_: kotlin.String?
      |
      |    data class Impl(override val value: kotlin.String?, override val value_: kotlin.String?) : com.example.UnionOfBoth {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |UnionOfBoth.Impl [
      |        |  value: ${"$"}value
      |        |  value_: ${"$"}value_
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `compatible adapter types merges nullability`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, nullif(value, 1 == 1)
      |FROM A;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.collections.List
      |
      |    val value_: kotlin.collections.List?
      |
      |    data class Impl(override val value: kotlin.collections.List, override val value_: kotlin.collections.List?) : com.example.UnionOfBoth {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |UnionOfBoth.Impl [
      |        |  value: ${"$"}value
      |        |  value_: ${"$"}value_
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `null type uses the other column in a union`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |unionOfBoth:
      |SELECT value, NULL
      |FROM A
      |UNION
      |SELECT NULL, value
      |FROM A;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.collections.List?
      |
      |    val expr: kotlin.collections.List?
      |
      |    data class Impl(override val value: kotlin.collections.List?, override val expr: kotlin.collections.List?) : com.example.UnionOfBoth {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |UnionOfBoth.Impl [
      |        |  value: ${"$"}value
      |        |  expr: ${"$"}expr
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `argument type uses the other column in a union`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, ?
      |FROM A
      |UNION
      |SELECT value, value
      |FROM A;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.collections.List
      |
      |    val expr: kotlin.collections.List
      |
      |    data class Impl(override val value: kotlin.collections.List, override val expr: kotlin.collections.List) : com.example.UnionOfBoth {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |UnionOfBoth.Impl [
      |        |  value: ${"$"}value
      |        |  expr: ${"$"}expr
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `union with enum adapter required works fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE TestBModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  address TEXT NOT NULL
      |);
      |CREATE TABLE TestAModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  address TEXT NOT NULL,
      |  status TEXT as TestADbModel.Status NOT NULL
      |);
      |
      |select_all:
      |SELECT *
      |FROM TestAModel
      |JOIN TestBModel ON TestAModel.name = TestBModel.name
      |
      |UNION
      |
      |SELECT *
      |FROM TestAModel
      |JOIN TestBModel ON TestAModel.address = TestBModel.address;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface Select_all {
      |    val _id: kotlin.Long
      |
      |    val name: kotlin.String
      |
      |    val address: kotlin.String
      |
      |    val status: TestADbModel.Status
      |
      |    val _id_: kotlin.Long
      |
      |    val name_: kotlin.String
      |
      |    val address_: kotlin.String
      |
      |    data class Impl(
      |        override val _id: kotlin.Long,
      |        override val name: kotlin.String,
      |        override val address: kotlin.String,
      |        override val status: TestADbModel.Status,
      |        override val _id_: kotlin.Long,
      |        override val name_: kotlin.String,
      |        override val address_: kotlin.String
      |    ) : com.example.Select_all {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |Select_all.Impl [
      |        |  _id: ${"$"}_id
      |        |  name: ${"$"}name
      |        |  address: ${"$"}address
      |        |  status: ${"$"}status
      |        |  _id_: ${"$"}_id_
      |        |  name_: ${"$"}name_
      |        |  address_: ${"$"}address_
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `non null column unioned with null in view`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE TestAModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL
      |);
      |CREATE TABLE TestBModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  nameB TEXT NOT NULL
      |);
      |
      |CREATE VIEW joined AS
      |SELECT _id, name, NULL AS nameB
      |FROM TestAModel
      |
      |UNION
      |
      |SELECT _id, NULL, nameB
      |FROM TestBModel;
      |
      |selectFromView:
      |SELECT name, nameB
      |FROM joined;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface SelectFromView {
      |    val name: kotlin.String?
      |
      |    val nameB: kotlin.String?
      |
      |    data class Impl(override val name: kotlin.String?, override val nameB: kotlin.String?) : com.example.SelectFromView {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |SelectFromView.Impl [
      |        |  name: ${"$"}name
      |        |  nameB: ${"$"}nameB
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `abstract class doesnt override kotlin functions unprepended by get`() {
    val result = FixtureCompiler.compileSql("""
      |someSelect:
      |SELECT '1' AS is_cool, '2' AS get_cheese, '3' AS stuff;
      |""".trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.String
      |
      |interface SomeSelect {
      |    val is_cool: String
      |
      |    val get_cheese: String
      |
      |    val stuff: String
      |
      |    data class Impl(
      |        override val is_cool: String,
      |        override val get_cheese: String,
      |        override val stuff: String
      |    ) : SomeSelect {
      |        override fun toString(): String = ""${'"'}
      |        |SomeSelect.Impl [
      |        |  is_cool: ${"$"}is_cool
      |        |  get_cheese: ${"$"}get_cheese
      |        |  stuff: ${"$"}stuff
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `adapted column in inner query`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE testA (
      |  id TEXT NOT NULL PRIMARY KEY,
      |  status TEXT AS Test.Status,
      |  attr TEXT
      |);
      |
      |someSelect:
      |SELECT *
      |FROM (
      |  SELECT *, 1 AS ordering
      |  FROM testA
      |  WHERE testA.attr IS NOT NULL
      |
      |  UNION
      |
      |  SELECT *, 2 AS ordering
      |         FROM testA
      |  WHERE testA.attr IS NULL
      |);
      |""".trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |interface SomeSelect {
      |    val id: String
      |
      |    val status: Test.Status?
      |
      |    val attr: String?
      |
      |    val ordering: Long
      |
      |    data class Impl(
      |        override val id: String,
      |        override val status: Test.Status?,
      |        override val attr: String?,
      |        override val ordering: Long
      |    ) : SomeSelect {
      |        override fun toString(): String = ""${'"'}
      |        |SomeSelect.Impl [
      |        |  id: ${"$"}id
      |        |  status: ${"$"}status
      |        |  attr: ${"$"}attr
      |        |  ordering: ${"$"}ordering
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `virtual table with tokenizer has correct types`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE VIRTUAL TABLE entity_fts USING fts4 (
      |  tokenize=simple X "${'$'} *&#%\'""\/(){}\[]|=+-_,:;<>-?!\t\r\n",
      |  text_content TEXT
      |);
      |
      |someSelect:
      |SELECT text_content, 1
      |FROM entity_fts;
      |""".trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |interface SomeSelect {
      |    val text_content: String?
      |
      |    val expr: Long
      |
      |    data class Impl(override val text_content: String?, override val expr: Long) : SomeSelect {
      |        override fun toString(): String = ""${'"'}
      |        |SomeSelect.Impl [
      |        |  text_content: ${"$"}text_content
      |        |  expr: ${"$"}expr
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `adapted column in foreign table exposed properly`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE testA (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  parent_id INTEGER NOT NULL,
      |  child_id INTEGER NOT NULL,
      |  FOREIGN KEY (parent_id) REFERENCES testB(_id),
      |  FOREIGN KEY (child_id) REFERENCES testB(_id)
      |);
      |
      |CREATE TABLE testB(
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  category TEXT AS java.util.List NOT NULL,
      |  type TEXT AS java.util.List NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |exact_match:
      |SELECT *
      |FROM testA
      |JOIN testB AS parentJoined ON parent_id = parentJoined._id
      |JOIN testB AS childJoined ON child_id = childJoined._id
      |WHERE parent_id = ? AND child_id = ?;
      """.trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/Exact_match.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import java.util.List
      |import kotlin.Long
      |import kotlin.String
      |
      |interface Exact_match {
      |    val _id: Long
      |
      |    val parent_id: Long
      |
      |    val child_id: Long
      |
      |    val _id_: Long
      |
      |    val category: List
      |
      |    val type: List
      |
      |    val name: String
      |
      |    val _id__: Long
      |
      |    val category_: List
      |
      |    val type_: List
      |
      |    val name_: String
      |
      |    data class Impl(
      |        override val _id: Long,
      |        override val parent_id: Long,
      |        override val child_id: Long,
      |        override val _id_: Long,
      |        override val category: List,
      |        override val type: List,
      |        override val name: String,
      |        override val _id__: Long,
      |        override val category_: List,
      |        override val type_: List,
      |        override val name_: String
      |    ) : Exact_match {
      |        override fun toString(): String = ""${'"'}
      |        |Exact_match.Impl [
      |        |  _id: ${"$"}_id
      |        |  parent_id: ${"$"}parent_id
      |        |  child_id: ${"$"}child_id
      |        |  _id_: ${"$"}_id_
      |        |  category: ${"$"}category
      |        |  type: ${"$"}type
      |        |  name: ${"$"}name
      |        |  _id__: ${"$"}_id__
      |        |  category_: ${"$"}category_
      |        |  type_: ${"$"}type_
      |        |  name_: ${"$"}name_
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `kotlin array types are printed properly`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE test (
      |  arrayValue BLOB AS kotlin.Array<kotlin.Int> NOT NULL,
      |  booleanArrayValue BLOB AS kotlin.BooleanArray NOT NULL,
      |  byteArrayValue BLOB AS kotlin.ByteArray NOT NULL,
      |  charArrayValue BLOB AS kotlin.CharArray NOT NULL,
      |  doubleArrayValue BLOB AS kotlin.DoubleArray NOT NULL,
      |  floatArrayValue BLOB AS kotlin.FloatArray NOT NULL,
      |  intArrayValue BLOB AS kotlin.IntArray NOT NULL,
      |  longArrayValue BLOB AS kotlin.LongArray NOT NULL,
      |  shortArrayValue BLOB AS kotlin.ShortArray NOT NULL
      |);
      |
      |selectAll:
      |SELECT *, 1
      |FROM test;
      |""".trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/SelectAll.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Array
      |import kotlin.BooleanArray
      |import kotlin.ByteArray
      |import kotlin.CharArray
      |import kotlin.DoubleArray
      |import kotlin.FloatArray
      |import kotlin.Int
      |import kotlin.IntArray
      |import kotlin.Long
      |import kotlin.LongArray
      |import kotlin.ShortArray
      |import kotlin.String
      |import kotlin.collections.contentToString
      |
      |interface SelectAll {
      |    val arrayValue: Array<Int>
      |
      |    val booleanArrayValue: BooleanArray
      |
      |    val byteArrayValue: ByteArray
      |
      |    val charArrayValue: CharArray
      |
      |    val doubleArrayValue: DoubleArray
      |
      |    val floatArrayValue: FloatArray
      |
      |    val intArrayValue: IntArray
      |
      |    val longArrayValue: LongArray
      |
      |    val shortArrayValue: ShortArray
      |
      |    val expr: Long
      |
      |    data class Impl(
      |        override val arrayValue: Array<Int>,
      |        override val booleanArrayValue: BooleanArray,
      |        override val byteArrayValue: ByteArray,
      |        override val charArrayValue: CharArray,
      |        override val doubleArrayValue: DoubleArray,
      |        override val floatArrayValue: FloatArray,
      |        override val intArrayValue: IntArray,
      |        override val longArrayValue: LongArray,
      |        override val shortArrayValue: ShortArray,
      |        override val expr: Long
      |    ) : SelectAll {
      |        override fun toString(): String = ""${'"'}
      |        |SelectAll.Impl [
      |        |  arrayValue: ${'$'}{arrayValue.contentToString()}
      |        |  booleanArrayValue: ${'$'}{booleanArrayValue.contentToString()}
      |        |  byteArrayValue: ${'$'}{byteArrayValue.contentToString()}
      |        |  charArrayValue: ${'$'}{charArrayValue.contentToString()}
      |        |  doubleArrayValue: ${'$'}{doubleArrayValue.contentToString()}
      |        |  floatArrayValue: ${'$'}{floatArrayValue.contentToString()}
      |        |  intArrayValue: ${'$'}{intArrayValue.contentToString()}
      |        |  longArrayValue: ${'$'}{longArrayValue.contentToString()}
      |        |  shortArrayValue: ${'$'}{shortArrayValue.contentToString()}
      |        |  expr: ${'$'}expr
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        "src/test/query-interface-fixtures/$fixtureRoot",
        SqlDelightCompiler::writeQueryInterfaces,
        false)
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText()).named(expectedFile.name).isEqualTo(
          actualOutput.toString())
    }
  }
}
