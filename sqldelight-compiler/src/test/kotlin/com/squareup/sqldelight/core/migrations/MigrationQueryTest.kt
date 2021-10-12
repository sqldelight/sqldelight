package com.squareup.sqldelight.core.migrations

import com.google.common.truth.Truth.assertWithMessage
import com.squareup.sqldelight.test.util.FixtureCompiler
import com.squareup.sqldelight.test.util.withInvariantLineSeparators
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MigrationQueryTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `alter table statements are reflected in queries`() {
    checkFixtureCompiles("alter-table")
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
      fixtureRoot = "src/test/migration-interface-fixtures/$fixtureRoot",
      generateDb = false,
      deriveSchemaFromMigrations = true
    )
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertWithMessage("No file with name $expectedFile").that(expectedFile.exists()).isTrue()
      assertWithMessage(expectedFile.name).that(actualOutput.toString())
        .isEqualTo(expectedFile.readText().withInvariantLineSeparators())
    }
  }
}
