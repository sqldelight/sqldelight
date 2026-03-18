package app.cash.sqldelight.core.views

import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.dialects.sqlite_3_38.SqliteDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withInvariantLineSeparators
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InterfaceGeneration {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun onlyTableType() {
    checkFixtureCompiles("only-table-type")
  }

  @Test fun requiresAdapter() {
    checkFixtureCompiles("requires-adapter")
  }

  @Test fun `view with exposed booleans through union`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  val INTEGER AS kotlin.Boolean NOT NULL
      |);
      |
      |CREATE VIEW someView AS
      |SELECT val, val
      |FROM test
      |UNION
      |SELECT 0, 0
      |FROM test;
      |
      """.trimMargin(),
      temporaryFolder,
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SomeView.kt"),
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Boolean
      |
      |public data class SomeView(
      |  public val val_: Boolean,
      |  public val val__: Boolean,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun `view with exposed booleans through union of separate tables`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  val INTEGER AS kotlin.Boolean NOT NULL
      |);
      |
      |CREATE TABLE another_test (
      |  val INTEGER AS kotlin.Boolean NOT NULL
      |);
      |
      |CREATE VIEW someView AS
      |SELECT val, val
      |FROM test
      |UNION
      |SELECT val, val
      |FROM another_test;
      |
      """.trimMargin(),
      temporaryFolder,
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SomeView.kt"),
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Boolean
      |
      |public data class SomeView(
      |  public val val_: Boolean,
      |  public val val__: Boolean,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun `view with inner cte compiles`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE Test (
      |  id INTEGER NOT NULL,
      |  txt TEXT NOT NULL
      |);
      |
      |CREATE VIEW SomeView AS
      |WITH SomeCte AS (
      |  SELECT id, txt
      |  FROM Test
      |)
      |SELECT id, txt
      |FROM SomeCte;
      |
      """.trimMargin(),
      temporaryFolder,
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/Test.kt"),
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class Test(
      |  public val id: Long,
      |  public val txt: String,
      |)
      |
      """.trimMargin(),
    )
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
      fixtureRoot = "src/test/view-interface-fixtures/$fixtureRoot",
      compilationMethod = { _, _, sqlDelightQueriesFile, writer ->
        SqlDelightCompiler.writeTableInterfaces(sqlDelightQueriesFile, writer)
      },
      generateDb = false,
    )
    assertThat(result.errors).isEmpty()
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertWithMessage("No file with name $expectedFile").that(expectedFile.exists()).isTrue()
      assertWithMessage(expectedFile.name)
        .that(actualOutput.toString())
        .isEqualTo(expectedFile.readText().withInvariantLineSeparators())
    }
  }
}
