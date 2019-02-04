package com.squareup.sqldelight.core.tables

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.compiler.TableInterfaceGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
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
      |interface Test {
      |    val annotated: @SomeAnnotation(cheese = ["havarti","provalone"], age = 10, type = List::class,
      |            otherAnnotation = SomeOtherAnnotation("value")) Int?
      |
      |    data class Impl(override val annotated: @SomeAnnotation(cheese = ["havarti","provalone"], age =
      |            10, type = List::class, otherAnnotation = SomeOtherAnnotation("value")) Int?) : Test {
      |        override fun toString(): String = ""${'"'}
      |        |Test.Impl [
      |        |  annotated: ${"$"}annotated
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
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
      |interface Test {
      |    val is_cool: String
      |
      |    val get_cheese: String?
      |
      |    val isle: String?
      |
      |    val stuff: String?
      |
      |    data class Impl(
      |        override val is_cool: String,
      |        override val get_cheese: String?,
      |        override val isle: String?,
      |        override val stuff: String?
      |    ) : Test {
      |        override fun toString(): String = ""${'"'}
      |        |Test.Impl [
      |        |  is_cool: ${"$"}is_cool
      |        |  get_cheese: ${"$"}get_cheese
      |        |  isle: ${"$"}isle
      |        |  stuff: ${"$"}stuff
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
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

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!)
    assertThat(generator.kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface Test {
      |    val intValue: kotlin.Int
      |
      |    val intValue2: kotlin.Int
      |
      |    val booleanValue: kotlin.Boolean
      |
      |    val shortValue: kotlin.Short
      |
      |    val longValue: kotlin.Long
      |
      |    val floatValue: kotlin.Float
      |
      |    val doubleValue: kotlin.Double
      |
      |    val blobValue: kotlin.ByteArray
      |
      |    data class Impl(
      |        override val intValue: kotlin.Int,
      |        override val intValue2: kotlin.Int,
      |        override val booleanValue: kotlin.Boolean,
      |        override val shortValue: kotlin.Short,
      |        override val longValue: kotlin.Long,
      |        override val floatValue: kotlin.Float,
      |        override val doubleValue: kotlin.Double,
      |        override val blobValue: kotlin.ByteArray
      |    ) : com.example.Test {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |Test.Impl [
      |        |  intValue: ${"$"}intValue
      |        |  intValue2: ${"$"}intValue2
      |        |  booleanValue: ${"$"}booleanValue
      |        |  shortValue: ${"$"}shortValue
      |        |  longValue: ${"$"}longValue
      |        |  floatValue: ${"$"}floatValue
      |        |  doubleValue: ${"$"}doubleValue
      |        |  blobValue: ${"$"}blobValue
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `complex generic type is inferred properly`() {
    val result = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  mapValue INTEGER AS kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>
      |);
      |""".trimMargin(), tempFolder)

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!)
    assertThat(generator.kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface Test {
      |    val mapValue: kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>?
      |
      |    class Adapter(internal val mapValueAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>, kotlin.Long>)
      |
      |    data class Impl(override val mapValue: kotlin.collections.Map<kotlin.collections.List<kotlin.collections.List<String>>, kotlin.collections.List<kotlin.collections.List<String>>>?) : com.example.Test {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |Test.Impl [
      |        |  mapValue: ${"$"}mapValue
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
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

    val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!)
    assertThat(generator.kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface Test {
      |    val _id: kotlin.Long
      |
      |    val enabledDays: kotlin.collections.Set<java.time.DayOfWeek>?
      |
      |    val enabledWeeks: kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>?
      |
      |    class Adapter(internal val enabledDaysAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.collections.Set<java.time.DayOfWeek>, kotlin.String>, internal val enabledWeeksAdapter: com.squareup.sqldelight.ColumnAdapter<kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>, kotlin.String>)
      |
      |    data class Impl(
      |        override val _id: kotlin.Long,
      |        override val enabledDays: kotlin.collections.Set<java.time.DayOfWeek>?,
      |        override val enabledWeeks: kotlin.collections.Set<com.gabrielittner.timetable.core.db.Week>?
      |    ) : com.example.Test {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |Test.Impl [
      |        |  _id: ${"$"}_id
      |        |  enabledDays: ${"$"}enabledDays
      |        |  enabledWeeks: ${"$"}enabledWeeks
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
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

        val generator = TableInterfaceGenerator(result.sqliteStatements().first().statement.createTableStmt!!)
        assertThat(generator.kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface Group {
      |    val index1: kotlin.String?
      |
      |    val index2: kotlin.String?
      |
      |    val index3: kotlin.String?
      |
      |    val index4: kotlin.String?
      |
      |    data class Impl(
      |        override val index1: kotlin.String?,
      |        override val index2: kotlin.String?,
      |        override val index3: kotlin.String?,
      |        override val index4: kotlin.String?
      |    ) : com.example.Group {
      |        override fun toString(): kotlin.String = ""${'"'}
      |        |Group.Impl [
      |        |  index1: ${"$"}index1
      |        |  index2: ${"$"}index2
      |        |  index3: ${"$"}index3
      |        |  index4: ${"$"}index4
      |        |]
      |        ""${'"'}.trimMargin()
      |    }
      |}
      |""".trimMargin())
    }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        "src/test/table-interface-fixtures/$fixtureRoot",
        SqlDelightCompiler::writeTableInterfaces,
        false)
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText()).named(expectedFile.name).isEqualTo(
          actualOutput.toString())
    }
  }
}
