package com.squareup.sqldelight.dialect

import com.google.common.truth.Truth
import com.squareup.sqldelight.assertions.FileSubject
import com.squareup.sqldelight.withCommonConfiguration
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class DialectIntegrationTests {

  @Test fun integrationTestsMySql() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-mysql"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsMySqlSchemaDefinitions() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-mysql-schema"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsMySqlSchemaOutput() {
    val integrationRoot = File("src/test/schema-output")

    val runner = GradleRunner.create()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "generateMainMyDatabaseMigrations", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")

    FileSubject.assertThat(File(integrationRoot, "build"))
      .contentsAreEqualTo(File(integrationRoot, "expected-build"))
  }

  @Test fun integrationTestsPostgreSql() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-postgresql"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsHsql() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-hsql"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
