package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class GenerateSchemaTest {
  @Test fun `schema file generates correctly`() {
    val fixtureRoot = File("src/test/schema-file")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .build()

    // verify
    assertThat(schemaFile.exists())
      .isTrue()

    schemaFile.delete()
  }

  @Test fun `generateSchema task can run twice`() {
    val fixtureRoot = File("src/test/schema-file")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .build()

    // verify
    assertThat(schemaFile.exists())
      .isTrue()
    val lastModified = schemaFile.lastModified()

    while (System.currentTimeMillis() - lastModified <= 1000) {
      // last modified only updates per second.
      Thread.yield()
    }

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--rerun-tasks", "generateMainDatabaseSchema", "--stacktrace")
      .build()

    // verify
    assertThat(schemaFile.exists()).isTrue()
    assertThat(schemaFile.lastModified()).isNotEqualTo(lastModified)

    schemaFile.delete()
  }

  @Test fun `schema file generates correctly with existing sqm files`() {
    val fixtureRoot = File("src/test/schema-file-sqm")

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .build()

    // verify
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/3.db")
    assertThat(schemaFile.exists())
      .isTrue()

    schemaFile.delete()
  }

  @Test fun `schema file generates correctly for android`() {
    val fixtureRoot = File("src/test/schema-file-android")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()
    schemaFile.deleteOnExit()

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateDebugDatabaseSchema", "--stacktrace")
      .build()

    // verify
    assertThat(schemaFile.exists()).isTrue()
  }

  @Test fun `driver initializer is executed`() {
    val fixtureRoot = File("src/test/migration-driver-initializer")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/2.db")
    if (schemaFile.exists()) schemaFile.delete()

    val output = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .build()

    // verify
    assertThat(output.output).contains("DriverInitializerImpl executed!")
    assertThat(output.output).contains("CustomDriver is used for connection.")
    assertThat(output.output).contains("BUILD SUCCESSFUL")
    assertThat(schemaFile.exists())
      .isTrue()

    schemaFile.delete()
  }
}
