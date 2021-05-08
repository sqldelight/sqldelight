package com.squareup.sqldelight.dialect

import com.google.common.truth.Truth
import com.squareup.sqldelight.assertions.FileSubject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class DialectIntegrationTests {

  @Test fun integrationTestsMySql() {
    val integrationRoot = File("src/test/integration-mysql")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
      .withProjectDir(integrationRoot)
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsMySqlSchemaDefinitions() {
    val integrationRoot = File("src/test/integration-mysql-schema")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
      .withProjectDir(integrationRoot)
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsMySqlSchemaOutput() {
    val integrationRoot = File("src/test/schema-output")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
      .withProjectDir(integrationRoot)
      .withArguments("clean", "generateMainMyDatabaseMigrations", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")

    FileSubject.assertThat(File(integrationRoot, "build"))
      .contentsAreEqualTo(File(integrationRoot, "expected-build"))
  }

  @Test fun integrationTestsPostgreSql() {
    val integrationRoot = File("src/test/integration-postgresql")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
      .withProjectDir(integrationRoot)
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsHsql() {
    val integrationRoot = File("src/test/integration-hsql")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
      .withProjectDir(integrationRoot)
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
