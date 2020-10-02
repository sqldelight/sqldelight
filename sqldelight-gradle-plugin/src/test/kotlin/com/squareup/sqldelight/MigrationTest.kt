package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class MigrationTest {
  @Test fun `failing migration errors properly`() {
    val fixtureRoot = File("src/test/migration-failure")

    val output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
        .buildAndFail()

    assertThat(output.output).contains("""
      |Error migrating from 1.db, fresh database looks different from migration database:
      |/tables[testView] - ADDED
      |/tables[test]/columns[test."value"]/ordinalPosition - CHANGED
      |/tables[test]/columns[test."value"]/partOfIndex - ADDED
      |/tables[test]/columns[test.value2]/attributes{IS_NULLABLE} - CHANGED
      |/tables[test]/columns[test.value2]/nullable - REMOVED
      |/tables[test]/columns[test.value2]/ordinalPosition - CHANGED
      |/tables[test]/indexes[test.testIndex] - ADDED
      |/tables[test]/triggers[test.testTrigger] - ADDED
      |""".trimMargin()
    )
  }

  @Test fun `migration file with errors reports file errors`() {
    val fixtureRoot = File("src/test/migration-syntax-failure")

    val output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
        .buildAndFail()

    assertThat(output.output).contains("""
      |1.sqm line 1:5 - TABLE expected, got 'TABE'
      |1    ALTER TABE test ADD COLUMN value2 TEXT
      """.trimMargin()
    )
  }

  @Test fun `deriving schema from migration introduces failures in migration files`() {
    val fixtureRoot = File("src/test/migration-schema-failure")

    val output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "build", "--stacktrace")
        .buildAndFail()

    assertThat(output.output).contains("""
      |1.sqm line 5:22 - No column found with name new_column
      |5    INSERT INTO test (id, new_column)
      |                           ^^^^^^^^^^
      |6    VALUES ("hello", "world")
      """.trimMargin()
    )
  }

  @Test fun `successful migration works properly`() {
    val fixtureRoot = File("src/test/migration-success")
    val gradleRoot = File(fixtureRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "check", "verifyMainDatabaseMigration", "--stacktrace")
        .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `multiple databases can have separate migrations`() {
    val fixtureRoot = File("src/test/multiple-project-migration-success")
    val gradleRoot = File(fixtureRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    var output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "check", "verifyMainDatabaseAMigration", "--stacktrace")
        .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")

    output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "check", "verifyMainDatabaseBMigration", "--stacktrace")
        .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `don't print java-object-diff warnings on default log level`() {
    val fixtureRoot = File("src/test/migration-success")

    val output = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
            .build()

    assertThat(output.output).doesNotContain("Detected circular reference in node at path")
  }

  @Test fun `print java-object-diff warnings on debug log level`() {
    val fixtureRoot = File("src/test/migration-success")

    val output = GradleRunner.create()
            .withProjectDir(fixtureRoot)
            .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace", "--debug")
            .build()

    assertThat(output.output).contains("""Detected circular reference in node at path /tables[test]/indexes[test.testIndex]/columns[test."value"]/index Going deeper would cause an infinite loop, so I'll stop looking at this instance along the current path.""")
  }

  @Test fun `migrations with a gap errors properly`() {
    val fixtureRoot = File("src/test/migration-gap-failure")

    val output = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
        .buildAndFail()

    assertThat(output.output).contains("""Gap in migrations detected. Expected migration 2, got 3.""")
  }
}
