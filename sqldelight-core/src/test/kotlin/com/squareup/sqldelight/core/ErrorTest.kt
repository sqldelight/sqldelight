package com.squareup.sqldelight.core;

import com.google.common.truth.Truth.assertWithMessage
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Test
import java.io.File

class ErrorTest {

  @Test fun duplicateSqliteIdentifiers() {
    checkCompilationFails("duplicate-sqlite-identifiers")
  }

  private fun checkCompilationFails(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture("src/test/errors/$fixtureRoot")
    val expectedFailure = File(result.fixtureRootDir, "failure.txt")
    if (expectedFailure.exists()) {
      assertWithMessage(result.sourceFiles).that(result.errors).containsExactlyElementsIn(
          expectedFailure.readText().split("\n").filterNot { it.isEmpty() })
    } else {
      assertWithMessage(result.sourceFiles).that(result.errors).isEmpty()
    }
  }
}
