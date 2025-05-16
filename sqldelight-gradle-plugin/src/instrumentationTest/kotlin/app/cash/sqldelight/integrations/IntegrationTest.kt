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
  @Test
  fun integrationTestsAndroid() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-android"))
      .withArguments("clean", "connectedCheck", "--stacktrace")
      .withDebug(true)

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
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
        },
      ).outcome,
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
        },
      ).outcome,
    ).isEqualTo(TaskOutcome.UP_TO_DATE)
  }

  @Test
  fun integrationTestsAndroidSingleVariant() {
    val projectLocation = File("src/test/integration-android-variants")
    val outputDir = File(projectLocation, "build/generated/sqldelight/code/QueryWrapper")
    val runner = GradleRunner.create()
      .withCommonConfiguration(projectLocation)
      .forwardOutput()

    val generateDebugResult = runner
      .withArguments("clean", "generateDebugQueryWrapperInterface", "-PdebugOnly=true")
      .build()
    assertThat(generateDebugResult.output).contains("BUILD SUCCESSFUL")

    assertThat(File(outputDir, "debug").exists()).isTrue()
  }

  @Test
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
