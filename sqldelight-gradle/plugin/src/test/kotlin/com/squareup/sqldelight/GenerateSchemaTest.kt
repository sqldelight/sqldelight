package com.squareup.sqldelight

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class GenerateSchemaTest {
  @Test fun `schema file generates correctly`() {
    val fixtureRoot = File("src/test/schema-file")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateSqlDelightSchema", "--stacktrace",
            "-Dsqldelight.skip.runtime=true")
        .build()

    // verify
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/1.db")
    Truth.assertThat(schemaFile.exists())
        .isTrue()

    schemaFile.delete()
  }
  @Test fun `schema file generates correctly with existing sqm files`() {
    val fixtureRoot = File("src/test/schema-file-sqm")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateSqlDelightSchema", "--stacktrace",
            "-Dsqldelight.skip.runtime=true")
        .build()

    // verify
    val schemaFile = File(fixtureRoot, "src/main/sqldelight/databases/3.db")
    Truth.assertThat(schemaFile.exists())
        .isTrue()

    schemaFile.delete()
  }
}