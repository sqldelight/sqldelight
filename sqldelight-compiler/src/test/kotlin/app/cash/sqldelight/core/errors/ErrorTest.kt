package app.cash.sqldelight.core.errors

import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.splitLines
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

class ErrorTest {

  @Test fun duplicateSqlIdentifiers() {
    checkCompilationFails("duplicate-sqlite-identifiers")
  }

  private fun checkCompilationFails(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture("src/test/errors/$fixtureRoot")
    val expectedFailure = File("src/test/errors/$fixtureRoot", "failure.txt")
    if (expectedFailure.exists()) {
      assertWithMessage(result.sourceFiles).that(result.errors).containsExactlyElementsIn(
        expectedFailure.readText().splitLines().filter { it.isNotEmpty() }
      )
    } else {
      assertWithMessage(result.sourceFiles).that(result.errors).isEmpty()
    }
  }
}
