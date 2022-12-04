package app.cash.sqldelight.core.tables

import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withInvariantLineSeparators
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class TesSerializerGeneration {
  @get:Rule
  val tempFolder = TemporaryFolder()

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
      fixtureRoot = "src/test/table-interface-fixtures/$fixtureRoot",
      compilationMethod = { _, _, sqlDelightQueriesFile, writer ->
        SqlDelightCompiler.writeTableInterfaces(sqlDelightQueriesFile, writer)
      },
      generateDb = false,
    )
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      Truth.assertWithMessage("No file with name $expectedFile").that(expectedFile.exists()).isTrue()
      Truth.assertWithMessage(expectedFile.name)
        .that(expectedFile.readText().withInvariantLineSeparators())
        .isEqualTo(actualOutput.toString())
    }
  }
}