package com.squareup.sqldelight

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
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
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
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
        .build()

    // verify
    assertThat(schemaFile.exists())
        .isTrue()

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "--rerun-tasks", "generateMainDatabaseSchema", "--stacktrace")
        .build()

    // verify
    assertThat(schemaFile.exists())
        .isTrue()

    schemaFile.delete()
  }

  @Test fun `schema file generates correctly with existing sqm files`() {
    val fixtureRoot = File("src/test/schema-file-sqm")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
        .build()

    // verify
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/3.db")
    assertThat(schemaFile.exists())
        .isTrue()

    schemaFile.delete()
  }

  @Test fun `schema file generates correctly for android`() {
    val fixtureRoot = File("src/test/schema-file")
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
        .build()

    // verify
    assertThat(schemaFile.exists()).isTrue()
  }
}
