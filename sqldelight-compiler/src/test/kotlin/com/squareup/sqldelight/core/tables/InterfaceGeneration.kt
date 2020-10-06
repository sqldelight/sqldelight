package com.squareup.sqldelight.core.tables

import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.FileSpec
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.compiler.TableInterfaceGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import com.squareup.sqldelight.test.util.withInvariantLineSeparators
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InterfaceGeneration {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun requiresAdapter() {
    checkFixtureCompiles("requires-adapter")
  }

  @Test fun `annotation with values is preserved`() {
    val result = FixtureCompiler.compileSql("""
      |import com.sample.SomeAnnotation;
      |import com.sample.SomeOtherAnnotation;
      |import java.util.List;
      |
      |CREATE TABLE test (
      |  annotated INTEGER AS @SomeAnnotation(
      |      cheese = ["havarti", "provalone"],
      |      age = 10,
      |      type = List::class,
      |      otherAnnotation = SomeOtherAnnotation("value")
      |  ) Int
      |);
      |""".trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import com.sample.SomeAnnotation
      |import com.sample.SomeOtherAnnotation
      |import java.util.List
      |import kotlin.Int
      |import kotlin.String
      |
      |data class Test(
      |  val annotated: @SomeAnnotation(cheese = ["havarti","provalone"], age = 10, type = List::class,
      |      otherAnnotation = SomeOtherAnnotation("value")) Int?
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |Test [
      |  |  annotated: ${"$"}annotated
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `abstract class doesnt override kotlin functions unprepended by get`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE test (
      |  is_cool TEXT NOT NULL,
      |  get_cheese TEXT,
      |  isle TEXT,
      |  stuff TEXT
      |);
      |""".trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.String
      |
      |data class Test(
      |  val is_cool: String,
      |  val get_cheese: String?,
      |  val isle: String?,
      |  val stuff: String?
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |Test [
      |  |  is_cool: ${"$"}is_cool
      |  |  get_cheese: ${"$"}get_cheese
      |  |  isle: ${"$"}isle
      |  |  stuff: ${"$"}stuff
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `kotlin types are inferred properly`() {
    val result = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  intValue INTEGER AS Int NOT NULL,
      |  intValue2 INTEGER AS Integer NOT NULL,
      |  booleanValue INTEGER AS Boolean NOT NULL,
      |  shortValue INTEGER AS Short NOT NULL,
      |  longValue INTEGER AS Long NOT NULL,
      |  floatValue REAL AS Float NOT NULL,
      |  doubleValue REAL AS Double NOT NULL,
      |  blobValue BLOB AS ByteArray NOT NULL
      |);
      |""".trimMargin(), tempFolder)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Test(
      |  val intValue: kotlin.Int,
      |  val intValue2: kotlin.Int,
      |  val booleanValue: kotlin.Boolean,
      |  val shortValue: kotlin.Short,
      |  val longValue: kotlin.Long,
      |  val floatValue: kotlin.Float,
      |  val doubleValue: kotlin.Double,
      |  val blobValue: kotlin.ByteArray
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Test [
      |  |  intValue: ${"$"}intValue
      |  |  intValue2: ${"$"}intValue2
      |  |  booleanValue: ${"$"}booleanValue
      |  |  shortValue: ${"$"}shortValue
      |  |  longValue: ${"$"}longValue
      |  |  floatValue: ${"$"}floatValue
      |  |  doubleValue: ${"$"}doubleValue
      |  |  blobValue: ${"$"}{blobValue.kotlin.collections.contentToString()}
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
  }

  @Test fun `kotlin array types are printed properly`() {
    val result = FixtureCompiler.parseSql("""
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
      |""".trimMargin(), tempFolder)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    val file = FileSpec.builder("", "Test")
        .addType(generator.kotlinImplementationSpec())
        .build()
    assertThat(file.toString()).isEqualTo("""
      |import com.squareup.sqldelight.ColumnAdapter
      |import kotlin.Array
      |import kotlin.BooleanArray
      |import kotlin.ByteArray
      |import kotlin.CharArray
      |import kotlin.DoubleArray
      |import kotlin.FloatArray
      |import kotlin.Int
      |import kotlin.IntArray
      |import kotlin.LongArray
      |import kotlin.ShortArray
      |import kotlin.String
      |import kotlin.collections.contentToString
      |
      |data class Test(
      |  val arrayValue: Array<Int>,
      |  val booleanArrayValue: BooleanArray,
      |  val byteArrayValue: ByteArray,
      |  val charArrayValue: CharArray,
      |  val doubleArrayValue: DoubleArray,
      |  val floatArrayValue: FloatArray,
      |  val intArrayValue: IntArray,
      |  val longArrayValue: LongArray,
      |  val shortArrayValue: ShortArray
      |) {
      |  override fun toString(): String = ""${'"'}
      |  |Test [
      |  |  arrayValue: ${'$'}{arrayValue.contentToString()}
      |  |  booleanArrayValue: ${'$'}{booleanArrayValue.contentToString()}
      |  |  byteArrayValue: ${'$'}{byteArrayValue.contentToString()}
      |  |  charArrayValue: ${'$'}{charArrayValue.contentToString()}
      |  |  doubleArrayValue: ${'$'}{doubleArrayValue.contentToString()}
      |  |  floatArrayValue: ${'$'}{floatArrayValue.contentToString()}
      |  |  intArrayValue: ${'$'}{intArrayValue.contentToString()}
      |  |  longArrayValue: ${'$'}{longArrayValue.contentToString()}
      |  |  shortArrayValue: ${'$'}{shortArrayValue.contentToString()}
      |  |]
      |  ""${'"'}.trimMargin()
      |
      |  class Adapter(
      |    val arrayValueAdapter: ColumnAdapter<Array<Int>, ByteArray>,
      |    val booleanArrayValueAdapter: ColumnAdapter<BooleanArray, ByteArray>,
      |    val byteArrayValueAdapter: ColumnAdapter<ByteArray, ByteArray>,
      |    val charArrayValueAdapter: ColumnAdapter<CharArray, ByteArray>,
      |    val doubleArrayValueAdapter: ColumnAdapter<DoubleArray, ByteArray>,
      |    val floatArrayValueAdapter: ColumnAdapter<FloatArray, ByteArray>,
      |    val intArrayValueAdapter: ColumnAdapter<IntArray, ByteArray>,
      |    val longArrayValueAdapter: ColumnAdapter<LongArray, ByteArray>,
      |    val shortArrayValueAdapter: ColumnAdapter<ShortArray, ByteArray>
      |  )
      |}
      |""".trimMargin())
  }

  @Test fun `complex generic type is inferred properly`() {
    val result = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  mapValue INTEGER AS kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>
      |);
      |""".trimMargin(), tempFolder)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Test(
      |  val mapValue: kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Test [
      |  |  mapValue: ${"$"}mapValue
      |  |]
      |  ""${'"'}.trimMargin()
      |
      |  class Adapter(
      |    val mapValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>, kotlin.Long>
      |  )
      |}
      |""".trimMargin())
  }

  @Test fun `type doesnt just use suffix to resolve`() {
    val result = FixtureCompiler.parseSql("""
      |import java.time.DayOfWeek;
      |import com.gabrielittner.timetable.core.db.Week;
      |import kotlin.collections.Set;
      |
      |CREATE TABLE test (
      |    _id INTEGER PRIMARY KEY AUTOINCREMENT,
      |    enabledDays TEXT as Set<DayOfWeek>,
      |    enabledWeeks TEXT as Set<Week>
      |);
      |""".trimMargin(), tempFolder)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Test(
      |  val _id: kotlin.Long,
      |  val enabledDays: kotlin.collections.Set<java.time.DayOfWeek>?,
      |  val enabledWeeks: kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Test [
      |  |  _id: ${"$"}_id
      |  |  enabledDays: ${"$"}enabledDays
      |  |  enabledWeeks: ${"$"}enabledWeeks
      |  |]
      |  ""${'"'}.trimMargin()
      |
      |  class Adapter(
      |    val enabledDaysAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.collections.Set<java.time.DayOfWeek>, kotlin.String>,
      |    val enabledWeeksAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>, kotlin.String>
      |  )
      |}
      |""".trimMargin())
  }

    @Test fun `escaped names is handled correctly`() {
        val result = FixtureCompiler.parseSql("""
      |CREATE TABLE [group] (
      |  `index1` TEXT,
      |  'index2' TEXT,
      |  "index3" TEXT,
      |  [index4] TEXT
      |);
      |""".trimMargin(), tempFolder)

        val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
        assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Group(
      |  val index1: kotlin.String?,
      |  val index2: kotlin.String?,
      |  val index3: kotlin.String?,
      |  val index4: kotlin.String?
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Group [
      |  |  index1: ${"$"}index1
      |  |  index2: ${"$"}index2
      |  |  index3: ${"$"}index3
      |  |  index4: ${"$"}index4
      |  |]
      |  ""${'"'}.trimMargin()
      |}
      |""".trimMargin())
    }

  @Test fun `underlying type is inferred properly in MySQL`() {
    val result = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  tinyIntValue TINYINT AS kotlin.Any NOT NULL,
      |  tinyIntBoolValue BOOLEAN AS kotlin.Any NOT NULL,
      |  smallIntValue SMALLINT AS kotlin.Any NOT NULL,
      |  mediumIntValue MEDIUMINT AS kotlin.Any NOT NULL,
      |  intValue INT AS kotlin.Any NOT NULL,
      |  bigIntValue BIGINT AS kotlin.Any NOT NULL,
      |  bitValue BIT AS kotlin.Any NOT NULL
      |);
      |""".trimMargin(), tempFolder, dialectPreset = DialectPreset.MYSQL)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Test(
      |  val tinyIntValue: kotlin.Any,
      |  val tinyIntBoolValue: kotlin.Any,
      |  val smallIntValue: kotlin.Any,
      |  val mediumIntValue: kotlin.Any,
      |  val intValue: kotlin.Any,
      |  val bigIntValue: kotlin.Any,
      |  val bitValue: kotlin.Any
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Test [
      |  |  tinyIntValue: ${"$"}tinyIntValue
      |  |  tinyIntBoolValue: ${"$"}tinyIntBoolValue
      |  |  smallIntValue: ${"$"}smallIntValue
      |  |  mediumIntValue: ${"$"}mediumIntValue
      |  |  intValue: ${"$"}intValue
      |  |  bigIntValue: ${"$"}bigIntValue
      |  |  bitValue: ${"$"}bitValue
      |  |]
      |  ""${'"'}.trimMargin()
      |
      |  class Adapter(
      |    val tinyIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Byte>,
      |    val tinyIntBoolValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Boolean>,
      |    val smallIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    val mediumIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    val intValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    val bigIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>,
      |    val bitValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Boolean>
      |  )
      |}
      |""".trimMargin())
  }

  @Test fun `underlying type is inferred properly in PostgreSQL`() {
    val result = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  smallIntValue SMALLINT AS kotlin.Any NOT NULL,
      |  intValue INT AS kotlin.Any NOT NULL,
      |  bigIntValue BIGINT AS kotlin.Any NOT NULL,
      |  smallSerialValue SMALLSERIAL AS kotlin.Any,
      |  serialValue SERIAL AS kotlin.Any NOT NULL,
      |  bigSerialValue BIGSERIAL AS kotlin.Any NOT NULL
      |);
      |""".trimMargin(), tempFolder, dialectPreset = DialectPreset.POSTGRESQL)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Test(
      |  val smallIntValue: kotlin.Any,
      |  val intValue: kotlin.Any,
      |  val bigIntValue: kotlin.Any,
      |  val smallSerialValue: kotlin.Any?,
      |  val serialValue: kotlin.Any,
      |  val bigSerialValue: kotlin.Any
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Test [
      |  |  smallIntValue: ${"$"}smallIntValue
      |  |  intValue: ${"$"}intValue
      |  |  bigIntValue: ${"$"}bigIntValue
      |  |  smallSerialValue: ${"$"}smallSerialValue
      |  |  serialValue: ${"$"}serialValue
      |  |  bigSerialValue: ${"$"}bigSerialValue
      |  |]
      |  ""${'"'}.trimMargin()
      |
      |  class Adapter(
      |    val smallIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    val intValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    val bigIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>,
      |    val smallSerialValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    val serialValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    val bigSerialValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>
      |  )
      |}
      |""".trimMargin())
  }

  @Test fun `underlying type is inferred properly in HSQL`() {
    val result = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  tinyIntValue TINYINT AS kotlin.Any NOT NULL,
      |  smallIntValue SMALLINT AS kotlin.Any NOT NULL,
      |  intValue INT AS kotlin.Any NOT NULL,
      |  bigIntValue BIGINT AS kotlin.Any NOT NULL,
      |  booleanValue BOOLEAN AS kotlin.Any NOT NULL
      |);
      |""".trimMargin(), tempFolder, dialectPreset = DialectPreset.MYSQL)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo("""
      |data class Test(
      |  val tinyIntValue: kotlin.Any,
      |  val smallIntValue: kotlin.Any,
      |  val intValue: kotlin.Any,
      |  val bigIntValue: kotlin.Any,
      |  val booleanValue: kotlin.Any
      |) {
      |  override fun toString(): kotlin.String = ""${'"'}
      |  |Test [
      |  |  tinyIntValue: ${"$"}tinyIntValue
      |  |  smallIntValue: ${"$"}smallIntValue
      |  |  intValue: ${"$"}intValue
      |  |  bigIntValue: ${"$"}bigIntValue
      |  |  booleanValue: ${"$"}booleanValue
      |  |]
      |  ""${'"'}.trimMargin()
      |
      |  class Adapter(
      |    val tinyIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Byte>,
      |    val smallIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    val intValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    val bigIntValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>,
      |    val booleanValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Boolean>
      |  )
      |}
      |""".trimMargin())
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        fixtureRoot = "src/test/table-interface-fixtures/$fixtureRoot",
        compilationMethod = { module, sqlDelightQueriesFile, folder, writer ->
          SqlDelightCompiler.writeTableInterfaces(module, sqlDelightQueriesFile, folder, writer)
        },
        generateDb = false
    )
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText().withInvariantLineSeparators())
          .named(expectedFile.name)
          .isEqualTo(actualOutput.toString())
    }
  }
}
