package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.QueryInterfaceGenerator
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
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
      |    data class Impl(override val val1: kotlin.String, override val val2: kotlin.String?) : com.example.LeftJoin
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
      |    data class Impl(override val value: kotlin.String, override val value_: kotlin.String) : com.example.LeftJoin
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
      |    data class Impl(override val value: kotlin.String?, override val value_: kotlin.String?) : com.example.UnionOfBoth
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
      |    data class Impl(override val value: kotlin.collections.List, override val value_: kotlin.collections.List?) : com.example.UnionOfBoth
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
      |    data class Impl(override val value: kotlin.collections.List?, override val expr: kotlin.collections.List?) : com.example.UnionOfBoth
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
      |    data class Impl(override val value: kotlin.collections.List, override val expr: kotlin.collections.List) : com.example.UnionOfBoth
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
      |            override val _id: kotlin.Long,
      |            override val name: kotlin.String,
      |            override val address: kotlin.String,
      |            override val status: TestADbModel.Status,
      |            override val _id_: kotlin.Long,
      |            override val name_: kotlin.String,
      |            override val address_: kotlin.String
      |    ) : com.example.Select_all
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
      |            override val is_cool: String,
      |            override val get_cheese: String,
      |            override val stuff: String
      |    ) : SomeSelect
      |}
      |
      |abstract class SomeSelectModel : SomeSelect {
      |    final override val get_cheese: String
      |        get() = get_cheese()
      |
      |    final override val stuff: String
      |        get() = stuff()
      |
      |    abstract fun get_cheese(): String
      |
      |    abstract fun stuff(): String
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
      |            override val id: String,
      |            override val status: Test.Status?,
      |            override val attr: String?,
      |            override val ordering: Long
      |    ) : SomeSelect
      |}
      |
      |abstract class SomeSelectModel : SomeSelect {
      |    final override val id: String
      |        get() = id()
      |
      |    final override val status: Test.Status?
      |        get() = status()
      |
      |    final override val attr: String?
      |        get() = attr()
      |
      |    final override val ordering: Long
      |        get() = ordering()
      |
      |    abstract fun id(): String
      |
      |    abstract fun status(): Test.Status?
      |
      |    abstract fun attr(): String?
      |
      |    abstract fun ordering(): Long
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
