package app.cash.sqldelight.core.tables

import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.compiler.TableInterfaceGenerator
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withInvariantLineSeparators
import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class InterfaceGeneration {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun requiresAdapter() {
    checkFixtureCompiles("requires-adapter")
  }

  @Test fun `annotation with values is preserved`() {
    val result = FixtureCompiler.compileSql(
      """
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
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import com.sample.SomeAnnotation
      |import com.sample.SomeOtherAnnotation
      |import java.util.List
      |import kotlin.Int
      |
      |public data class Test(
      |  public val annotated: @SomeAnnotation(cheese = ["havarti","provalone"], age = 10, type =
      |      List::class, otherAnnotation = SomeOtherAnnotation("value")) Int?
      |)
      |""".trimMargin()
    )
  }

  @Test fun `abstract class doesnt override kotlin functions unprepended by get`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  is_cool TEXT NOT NULL,
      |  get_cheese TEXT,
      |  isle TEXT,
      |  stuff TEXT
      |);
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.String
      |
      |public data class Test(
      |  public val is_cool: String,
      |  public val get_cheese: String?,
      |  public val isle: String?,
      |  public val stuff: String?
      |)
      |""".trimMargin()
    )
  }

  @Test fun `kotlin types are inferred properly`() {
    val result = FixtureCompiler.parseSql(
      """
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
      |""".trimMargin(),
      tempFolder
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Test(
      |  public val intValue: kotlin.Int,
      |  public val intValue2: kotlin.Int,
      |  public val booleanValue: kotlin.Boolean,
      |  public val shortValue: kotlin.Short,
      |  public val longValue: kotlin.Long,
      |  public val floatValue: kotlin.Float,
      |  public val doubleValue: kotlin.Double,
      |  public val blobValue: kotlin.ByteArray
      |)
      |""".trimMargin()
    )
  }

  @Test fun `complex generic type is inferred properly`() {
    val result = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  mapValue INTEGER AS kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>
      |);
      |""".trimMargin(),
      tempFolder
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Test(
      |  public val mapValue: kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>?
      |) {
      |  public class Adapter(
      |    public val mapValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>, kotlin.Long>
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `type doesnt just use suffix to resolve`() {
    val result = FixtureCompiler.parseSql(
      """
      |import java.time.DayOfWeek;
      |import com.gabrielittner.timetable.core.db.Week;
      |import kotlin.collections.Set;
      |
      |CREATE TABLE test (
      |    _id INTEGER PRIMARY KEY AUTOINCREMENT,
      |    enabledDays TEXT AS Set<DayOfWeek>,
      |    enabledWeeks TEXT AS Set<Week>
      |);
      |""".trimMargin(),
      tempFolder
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Test(
      |  public val _id: kotlin.Long,
      |  public val enabledDays: kotlin.collections.Set<java.time.DayOfWeek>?,
      |  public val enabledWeeks: kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>?
      |) {
      |  public class Adapter(
      |    public val enabledDaysAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.collections.Set<java.time.DayOfWeek>, kotlin.String>,
      |    public val enabledWeeksAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>, kotlin.String>
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `escaped names is handled correctly`() {
    val result = FixtureCompiler.parseSql(
      """
      |CREATE TABLE [group] (
      |  `index1` TEXT,
      |  'index2' TEXT,
      |  "index3" TEXT,
      |  [index4] TEXT
      |);
      |""".trimMargin(),
      tempFolder
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Group(
      |  public val index1: kotlin.String?,
      |  public val index2: kotlin.String?,
      |  public val index3: kotlin.String?,
      |  public val index4: kotlin.String?
      |)
      |""".trimMargin()
    )
  }

  @Test fun `underlying type is inferred properly in MySQL`() {
    val result = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  tinyIntValue TINYINT AS kotlin.Any NOT NULL,
      |  tinyIntBoolValue BOOLEAN AS kotlin.Any NOT NULL,
      |  smallIntValue SMALLINT AS kotlin.Any NOT NULL,
      |  mediumIntValue MEDIUMINT AS kotlin.Any NOT NULL,
      |  intValue INT AS kotlin.Any NOT NULL,
      |  bigIntValue BIGINT AS kotlin.Any NOT NULL,
      |  bitValue BIT AS kotlin.Any NOT NULL
      |);
      |""".trimMargin(),
      tempFolder, dialectPreset = DialectPreset.MYSQL
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Test(
      |  public val tinyIntValue: kotlin.Any,
      |  public val tinyIntBoolValue: kotlin.Any,
      |  public val smallIntValue: kotlin.Any,
      |  public val mediumIntValue: kotlin.Any,
      |  public val intValue: kotlin.Any,
      |  public val bigIntValue: kotlin.Any,
      |  public val bitValue: kotlin.Any
      |) {
      |  public class Adapter(
      |    public val tinyIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Byte>,
      |    public val tinyIntBoolValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Boolean>,
      |    public val smallIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    public val mediumIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    public val intValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    public val bigIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>,
      |    public val bitValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Boolean>
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `underlying type is inferred properly in PostgreSQL`() {
    val result = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  smallIntValue SMALLINT AS kotlin.Any NOT NULL,
      |  intValue INT AS kotlin.Any NOT NULL,
      |  bigIntValue BIGINT AS kotlin.Any NOT NULL,
      |  smallSerialValue SMALLSERIAL AS kotlin.Any,
      |  serialValue SERIAL AS kotlin.Any NOT NULL,
      |  bigSerialValue BIGSERIAL AS kotlin.Any NOT NULL
      |);
      |""".trimMargin(),
      tempFolder, dialectPreset = DialectPreset.POSTGRESQL
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Test(
      |  public val smallIntValue: kotlin.Any,
      |  public val intValue: kotlin.Any,
      |  public val bigIntValue: kotlin.Any,
      |  public val smallSerialValue: kotlin.Any?,
      |  public val serialValue: kotlin.Any,
      |  public val bigSerialValue: kotlin.Any
      |) {
      |  public class Adapter(
      |    public val smallIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    public val intValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    public val bigIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>,
      |    public val smallSerialValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    public val serialValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    public val bigSerialValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `underlying type is inferred properly in HSQL`() {
    val result = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  tinyIntValue TINYINT AS kotlin.Any NOT NULL,
      |  smallIntValue SMALLINT AS kotlin.Any NOT NULL,
      |  intValue INT AS kotlin.Any NOT NULL,
      |  bigIntValue BIGINT AS kotlin.Any NOT NULL,
      |  booleanValue BOOLEAN AS kotlin.Any NOT NULL
      |);
      |""".trimMargin(),
      tempFolder, dialectPreset = DialectPreset.MYSQL
    )

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!.tableExposed())
    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Test(
      |  public val tinyIntValue: kotlin.Any,
      |  public val smallIntValue: kotlin.Any,
      |  public val intValue: kotlin.Any,
      |  public val bigIntValue: kotlin.Any,
      |  public val booleanValue: kotlin.Any
      |) {
      |  public class Adapter(
      |    public val tinyIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Byte>,
      |    public val smallIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Short>,
      |    public val intValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Int>,
      |    public val bigIntValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Long>,
      |    public val booleanValueAdapter: app.cash.sqldelight.ColumnAdapter<kotlin.Any, kotlin.Boolean>
      |  )
      |}
      |""".trimMargin()
    )
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
      fixtureRoot = "src/test/table-interface-fixtures/$fixtureRoot",
      compilationMethod = { _, sqlDelightQueriesFile, writer ->
        SqlDelightCompiler.writeTableInterfaces(sqlDelightQueriesFile, writer)
      },
      generateDb = false
    )
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertWithMessage("No file with name $expectedFile").that(expectedFile.exists()).isTrue()
      assertWithMessage(expectedFile.name)
        .that(expectedFile.readText().withInvariantLineSeparators())
        .isEqualTo(actualOutput.toString())
    }
  }
}
