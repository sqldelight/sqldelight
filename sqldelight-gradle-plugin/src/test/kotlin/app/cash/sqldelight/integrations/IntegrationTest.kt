/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.integrations

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class IntegrationTest {
  @Test fun integrationTests() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsSqliteJsonModule() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-sqlite-json"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun migrationCallbackIntegrationTests() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-migration-callbacks"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `sqldelight output is cacheable`() {
    val fixtureRoot = File("src/test/integration")
    fixtureRoot.resolve("build").deleteRecursively()
    fixtureRoot.resolve("build-cache").deleteRecursively()
    val gradleRunner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)

    val firstRun = gradleRunner
      .withArguments("build", "--build-cache", "--stacktrace")
      .build()

    with(firstRun.task(":generateMainQueryWrapperInterface")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isNotEqualTo(TaskOutcome.FROM_CACHE)
    }

    fixtureRoot.resolve("build").deleteRecursively()

    val secondRun = gradleRunner
      .withArguments("build", "--build-cache", "--stacktrace")
      .build()

    with(secondRun.task(":generateMainQueryWrapperInterface")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
    }

    fixtureRoot.resolve("build").deleteRecursively()
    fixtureRoot.resolve("build-cache").deleteRecursively()
  }

  @Test fun `sqldelight output is cacheable when in different directories`() {
    val fixtureRoot = File("src/test/integration")
    fixtureRoot.resolve("build").deleteRecursively()
    fixtureRoot.resolve("build-cache").deleteRecursively()
    val gradleRunner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)

    val firstRun = gradleRunner
      .withArguments("build", "--build-cache", "-Dorg.gradle.caching.debug=true")
      .forwardOutput()
      .build()

    with(firstRun.task(":generateMainQueryWrapperInterface")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isNotEqualTo(TaskOutcome.FROM_CACHE)
    }

    fixtureRoot.resolve("build").deleteRecursively()

    // Create a clone of the project
    val clonedRoot = File("src/test/integration-clone")
    clonedRoot.deleteRecursively()
    fixtureRoot.copyRecursively(clonedRoot)
    val settingsFile = File(clonedRoot, "settings.gradle")
    settingsFile.writeText(settingsFile.readText().replace("build-cache", "../integration/build-cache"))

    val secondRun = GradleRunner.create()
      .withCommonConfiguration(clonedRoot)
      .withArguments("build", "--build-cache", "-Dorg.gradle.caching.debug=true")
      .forwardOutput()
      .build()

    with(secondRun.task(":generateMainQueryWrapperInterface")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
    }

    fixtureRoot.resolve("build").deleteRecursively()
    fixtureRoot.resolve("build-cache").deleteRecursively()
    clonedRoot.deleteRecursively()
  }

  @Test fun integrationTests_multithreaded_sqlite() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multithreaded-sqlite"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsSqlite_3_24() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-sqlite-3-24"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsSqlite_3_35() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-sqlite-3-35"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `integration test multiplatform project with async generated code`() {
    val integrationRoot = File("src/test/integration-multiplatform-async")

    val runner = GradleRunner.create()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "test", "--stacktrace")
      .withDebug(true)

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun adapterQuestionmarkTests() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/adapter-questionmark"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun selectWithoutFromTest() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/selectWithoutFrom"))
      .withArguments("clean", "assemble", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `deriveSchemaFromMigrations creates a db without queries`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/derive-schema-no-queries"))
      .withArguments("clean", "build", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
