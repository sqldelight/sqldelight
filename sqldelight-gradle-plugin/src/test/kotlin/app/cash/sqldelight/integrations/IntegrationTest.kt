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

import app.cash.sqldelight.Instrumentation
import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File

class IntegrationTest {
  @Test fun integrationTests() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration"))
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

  @Test @Category(Instrumentation::class) fun integrationTestsAndroid() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-android"))
      .withArguments("clean", "connectedCheck", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  @Category(Instrumentation::class)
  fun integrationTestsAndroidVariants() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-android-variants"))
      .forwardOutput()

    val generateDebugResult = runner
      .withArguments("clean", "generateDebugQueryWrapperInterface")
      .build()
    assertThat(generateDebugResult.output).contains("BUILD SUCCESSFUL")

    val generateReleaseResult = runner
      .withArguments("generateReleaseQueryWrapperInterface")
      .build()
    assertThat(generateReleaseResult.output).contains("BUILD SUCCESSFUL")

    val testDebugResult = runner
      .withArguments("testDebugUnitTest")
      .build()
    assertThat(testDebugResult.output).contains("BUILD SUCCESSFUL")
    assertThat(
      requireNotNull(
        testDebugResult.task(":generateDebugQueryWrapperInterface"),
        {
          "Could not find task in ${testDebugResult.tasks}"
        }
      ).outcome
    ).isEqualTo(TaskOutcome.UP_TO_DATE)

    val testReleaseResult = runner
      .withArguments("testReleaseUnitTest")
      .build()
    assertThat(testReleaseResult.output).contains("BUILD SUCCESSFUL")
    assertThat(
      requireNotNull(
        testReleaseResult.task(":generateReleaseQueryWrapperInterface"),
        {
          "Could not find task in ${testDebugResult.tasks}"
        }
      ).outcome
    ).isEqualTo(TaskOutcome.UP_TO_DATE)
  }

  @Test
  @Category(Instrumentation::class)
  fun integrationTestsAndroidLibrary() {
    val integrationRoot = File("src/test/integration-android-library")

    // Copy the normal android integration files over.
    val target = File(integrationRoot, "src")
    File(File("src/test/integration-android"), "src").copyRecursively(target)
    try {
      val runner = GradleRunner.create()
        .withCommonConfiguration(integrationRoot)
        .withArguments("clean", "connectedCheck", "--stacktrace")

      val result = runner.build()
      assertThat(result.output).contains("BUILD SUCCESSFUL")
    } finally {
      target.deleteRecursively()
    }
  }

  @Test
  @Category(Instrumentation::class)
  fun `integration test android target of a multiplatform project`() {
    val integrationRoot = File("src/test/integration-multiplatform")
    val buildGradle = File(integrationRoot, "build.gradle").apply { deleteOnExit() }
    File(integrationRoot, "android-build.gradle").copyTo(buildGradle, overwrite = true)

    val runner = GradleRunner.create()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "test", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  @Category(Instrumentation::class)
  fun `integration test ios target of a multiplatform project`() {
    val integrationRoot = File("src/test/integration-multiplatform")
    val buildGradle = File(integrationRoot, "build.gradle").apply { deleteOnExit() }
    File(integrationRoot, "ios-build.gradle").copyTo(buildGradle, overwrite = true)

    val runner = GradleRunner.create()
      .forwardOutput()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "iosTest", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  @Category(Instrumentation::class)
  fun `integration metadata task compiles successfully`() {
    val integrationRoot = File("src/test/integration-multiplatform")
    val buildGradle = File(integrationRoot, "build.gradle").apply { deleteOnExit() }
    File(integrationRoot, "ios-build.gradle").copyTo(buildGradle, overwrite = true)

    val runner = GradleRunner.create()
      .forwardOutput()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "compileKotlinMetadata", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
