package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class FailureTest {
  @Test fun `missing package directory fails properly`() {
    val fixtureRoot = File("src/test/no-package")

    val output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
        .buildAndFail()

    assertThat(output.output).contains("""
      |NoPackage.sq line 1:0 - SqlDelight files must be placed in a package directory.
      |1    CREATE TABLE test (
      |2      value TEXT
      |3    );
      |""".trimMargin()
    )
  }
}
