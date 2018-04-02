package com.squareup.sqldelight.core.views

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class InterfaceGeneration {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun onlyTableType() {
    checkFixtureCompiles("only-table-type")
  }

  @Test fun requiresAdapter() {
    checkFixtureCompiles("requires-adapter")
  }

  @Test fun `view with exposed booleans through union`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE test (
      |  val INTEGER AS Boolean NOT NULL
      |);
      |
      |CREATE VIEW someView AS
      |SELECT val, val
      |FROM test
      |UNION
      |SELECT 0, 0
      |FROM test;
      |""".trimMargin(), temporaryFolder)

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
        File(result.outputDirectory, "com/example/SomeView.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo("""
      |package com.example
      |
      |import kotlin.Boolean
      |
      |interface SomeView {
      |    val val_: Boolean
      |
      |    val val__: Boolean
      |
      |    data class Impl(override val val_: Boolean, override val val__: Boolean) : SomeView
      |}
      |
      |abstract class SomeViewModel : SomeView {
      |    final override val val_: Boolean
      |        get() = val_()
      |
      |    final override val val__: Boolean
      |        get() = val__()
      |
      |    abstract fun val_(): Boolean
      |
      |    abstract fun val__(): Boolean
      |}
      |""".trimMargin())
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        "src/test/view-interface-fixtures/$fixtureRoot",
        SqlDelightCompiler::writeViewInterfaces,
        false)
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText()).named(expectedFile.name).isEqualTo(
          actualOutput.toString())
    }
  }
}
