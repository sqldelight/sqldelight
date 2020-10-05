package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.QueryInterfaceGenerator
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.test.util.FixtureCompiler
import com.squareup.sqldelight.test.util.withInvariantLineSeparators
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class LeftJoin(
      |  val val1: kotlin.String,
      |  val val2: kotlin.String?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |LeftJoin [
      |  |  val1: ${"$"}val1
      |  |  val2: ${"$"}val2
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class LeftJoin(
      |  val value: kotlin.String,
      |  val value_: kotlin.String
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |LeftJoin [
      |  |  value: ${"$"}value
      |  |  value_: ${"$"}value_
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class UnionOfBoth(
      |  val value: kotlin.String?,
      |  val value_: kotlin.String?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |UnionOfBoth [
      |  |  value: ${"$"}value
      |  |  value_: ${"$"}value_
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class UnionOfBoth(
      |  val value: kotlin.collections.List,
      |  val value_: kotlin.collections.List?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |UnionOfBoth [
      |  |  value: ${"$"}value
      |  |  value_: ${"$"}value_
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class UnionOfBoth(
      |  val value: kotlin.collections.List?,
      |  val expr: kotlin.collections.List?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |UnionOfBoth [
      |  |  value: ${"$"}value
      |  |  expr: ${"$"}expr
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class UnionOfBoth(
      |  val value: kotlin.collections.List,
      |  val expr: kotlin.collections.List
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |UnionOfBoth [
      |  |  value: ${"$"}value
      |  |  expr: ${"$"}expr
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Select_all(
      |  val _id: kotlin.Long,
      |  val name: kotlin.String,
      |  val address: kotlin.String,
      |  val status: TestADbModel.Status,
      |  val _id_: kotlin.Long,
      |  val name_: kotlin.String,
      |  val address_: kotlin.String
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Select_all [
      |  |  _id: ${"$"}_id
      |  |  name: ${"$"}name
      |  |  address: ${"$"}address
      |  |  status: ${"$"}status
      |  |  _id_: ${"$"}_id_
      |  |  name_: ${"$"}name_
      |  |  address_: ${"$"}address_
      |  |]
      |  ""${'"'}.trimMargin()
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
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class SelectFromView(
      |  val name: kotlin.String?,
      |  val nameB: kotlin.String?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |SelectFromView [
      |  |  name: ${"$"}name
      |  |  nameB: ${"$"}nameB
      |  |]
      |  ""${'"'}.trimMargin()
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
      |data class SomeSelect(
      |  val is_cool: String,
      |  val get_cheese: String,
      |  val stuff: String
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |SomeSelect [
      |  |  is_cool: ${"$"}is_cool
      |  |  get_cheese: ${"$"}get_cheese
      |  |  stuff: ${"$"}stuff
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `adapted column in inner query`() {
    val result = FixtureCompiler.compileSql("""
      |import com.example.Test;
      |
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
      |data class SomeSelect(
      |  val id: String,
      |  val status: Test.Status?,
      |  val attr: String?,
      |  val ordering: Long
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |SomeSelect [
      |  |  id: ${"$"}id
      |  |  status: ${"$"}status
      |  |  attr: ${"$"}attr
      |  |  ordering: ${"$"}ordering
      |  |]
      |  ""${'"'}.trimMargin()
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
      |data class SomeSelect(
      |  val text_content: String?,
      |  val expr: Long
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |SomeSelect [
      |  |  text_content: ${"$"}text_content
      |  |  expr: ${"$"}expr
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `fts5 virtual table with tokenizer has correct types`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE VIRTUAL TABLE entity_fts USING fts5 (
      |  text_content TEXT,
      |  prefix='2 3 4 5 6 7',
      |  content_rowid=id
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
      |data class SomeSelect(
      |  val text_content: String?,
      |  val expr: Long
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |SomeSelect [
      |  |  text_content: ${"$"}text_content
      |  |  expr: ${'$'}expr
      |  |]
      |  ""${'"'}.trimMargin()
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
      |data class Exact_match(
      |  val _id: Long,
      |  val parent_id: Long,
      |  val child_id: Long,
      |  val _id_: Long,
      |  val category: List,
      |  val type: List,
      |  val name: String,
      |  val _id__: Long,
      |  val category_: List,
      |  val type_: List,
      |  val name_: String
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |Exact_match [
      |  |  _id: ${"$"}_id
      |  |  parent_id: ${"$"}parent_id
      |  |  child_id: ${"$"}child_id
      |  |  _id_: ${"$"}_id_
      |  |  category: ${"$"}category
      |  |  type: ${"$"}type
      |  |  name: ${"$"}name
      |  |  _id__: ${"$"}_id__
      |  |  category_: ${"$"}category_
      |  |  type_: ${"$"}type_
      |  |  name_: ${"$"}name_
      |  |]
      |  ""${'"'}.trimMargin()
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
      |data class SelectAll(
      |  val arrayValue: Array<Int>,
      |  val booleanArrayValue: BooleanArray,
      |  val byteArrayValue: ByteArray,
      |  val charArrayValue: CharArray,
      |  val doubleArrayValue: DoubleArray,
      |  val floatArrayValue: FloatArray,
      |  val intArrayValue: IntArray,
      |  val longArrayValue: LongArray,
      |  val shortArrayValue: ShortArray,
      |  val expr: Long
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |SelectAll [
      |  |  arrayValue: ${'$'}{arrayValue.contentToString()}
      |  |  booleanArrayValue: ${'$'}{booleanArrayValue.contentToString()}
      |  |  byteArrayValue: ${'$'}{byteArrayValue.contentToString()}
      |  |  charArrayValue: ${'$'}{charArrayValue.contentToString()}
      |  |  doubleArrayValue: ${'$'}{doubleArrayValue.contentToString()}
      |  |  floatArrayValue: ${'$'}{floatArrayValue.contentToString()}
      |  |  intArrayValue: ${'$'}{intArrayValue.contentToString()}
      |  |  longArrayValue: ${'$'}{longArrayValue.contentToString()}
      |  |  shortArrayValue: ${'$'}{shortArrayValue.contentToString()}
      |  |  expr: ${'$'}expr
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `avg aggregate has proper nullable type`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE test (
      |  integer_value INTEGER NOT NULL,
      |  real_value REAL NOT NULL,
      |  nullable_real_value REAL
      |);
      |
      |average:
      |SELECT
      |  avg(integer_value) AS avg_integer_value,
      |  avg(real_value) AS avg_real_value,
      |  avg(nullable_real_value) AS avg_nullable_real_value
      |FROM test;
      |""".trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/Average.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Double
      |import kotlin.String
      |
      |data class Average(
      |  val avg_integer_value: Double?,
      |  val avg_real_value: Double?,
      |  val avg_nullable_real_value: Double?
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |Average [
      |  |  avg_integer_value: ${'$'}avg_integer_value
      |  |  avg_real_value: ${'$'}avg_real_value
      |  |  avg_nullable_real_value: ${'$'}avg_nullable_real_value
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `group_concat properly inherits nullability`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE target (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  coacheeId INTEGER NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |CREATE TABLE challengeTarget (
      |  targetId INTEGER NOT NULL,
      |  challengeId INTEGER NOT NULL
      |);
      |
      |CREATE TABLE challenge (
      |  id INTEGER NOT NULL,
      |  cancelledAt INTEGER,
      |  emoji TEXT
      |);
      |
      |targetWithEmojis:
      |SELECT target.id AS id, target.name AS name, GROUP_CONCAT(challenge.emoji, "") AS emojis
      |  FROM target
      |  LEFT JOIN challengeTarget
      |    ON challengeTarget.targetId = target.id
      |  LEFT JOIN challenge
      |    ON challengeTarget.challengeId = challenge.id AND challenge.cancelledAt IS NULL
      |  WHERE target.coacheeId = ?
      |  GROUP BY 1
      |  ORDER BY target.name COLLATE NOCASE ASC
      |;
    """.trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/TargetWithEmojis.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |data class TargetWithEmojis(
      |  val id: Long,
      |  val name: String,
      |  val emojis: String?
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |TargetWithEmojis [
      |  |  id: ${'$'}id
      |  |  name: ${'$'}name
      |  |  emojis: ${'$'}emojis
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `cast inherits nullability`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE example (
      |  foo TEXT
      |);
      |
      |selectWithCast:
      |SELECT
      |  foo,
      |  CAST(foo AS BLOB) AS bar
      |FROM example;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo("""
      |data class SelectWithCast(
      |  val foo: kotlin.String?,
      |  val bar: kotlin.ByteArray?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |SelectWithCast [
      |  |  foo: ${'$'}foo
      |  |  bar: ${'$'}bar
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        fixtureRoot = "src/test/query-interface-fixtures/$fixtureRoot",
        compilationMethod = SqlDelightCompiler::writeQueryInterfaces,
        generateDb = false
    )
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(actualOutput.toString())
          .named(expectedFile.name)
          .isEqualTo(expectedFile.readText().withInvariantLineSeparators())
    }
  }
}
