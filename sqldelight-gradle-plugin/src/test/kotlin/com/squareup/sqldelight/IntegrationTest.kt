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
package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class IntegrationTest {
  @Test fun integrationTests() {
    val integrationRoot = File("src/test/integration")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
  @Test fun integrationTestsSqlite_3_24() {
    val integrationRoot = File("src/test/integration-sqlite-3-24")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsAndroid() {
    val androidHome = androidHome()
    val integrationRoot = File("src/test/integration-android")
    File(integrationRoot, "local.properties").writeText("sdk.dir=$androidHome\n")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", "connectedCheck", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsAndroidLibrary() {
    val androidHome = androidHome()
    val integrationRoot = File("src/test/integration-android-library")

    // Copy the normal android integration files over.
    val target = File(integrationRoot, "src")
    File(File("src/test/integration-android"), "src").copyRecursively(target)
    try {

      File(integrationRoot, "local.properties").writeText("sdk.dir=$androidHome\n")
      val gradleRoot = File(integrationRoot, "gradle").apply {
        mkdir()
      }
      File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

      val runner = GradleRunner.create()
          .withProjectDir(integrationRoot)
          .withPluginClasspath()
          .withArguments("clean", "connectedCheck", "--stacktrace")

      val result = runner.build()
      assertThat(result.output).contains("BUILD SUCCESSFUL")
    } finally {
      target.deleteRecursively()
    }
  }

  @Test fun `integration test android target of a multiplatform project`() {
    val androidHome = androidHome()
    val integrationRoot = File("src/test/integration-multiplatform")
    val buildGradle = File(integrationRoot, "build.gradle").apply { deleteOnExit() }
    File(integrationRoot, "local.properties").writeText("sdk.dir=$androidHome\n")
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
    File(integrationRoot, "android-build.gradle").copyTo(buildGradle, overwrite = true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", "test", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `integration test ios target of a multiplatform project`() {
    val integrationRoot = File("src/test/integration-multiplatform")
    val buildGradle = File(integrationRoot, "build.gradle").apply { deleteOnExit() }
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
    File(integrationRoot, "ios-build.gradle").copyTo(buildGradle, overwrite = true)

    val runner = GradleRunner.create()
        .forwardOutput()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", "iosTest", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `integration metadata task compiles successfully`() {
    val integrationRoot = File("src/test/integration-multiplatform")
    val buildGradle = File(integrationRoot, "build.gradle").apply { deleteOnExit() }
    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
    File(integrationRoot, "ios-build.gradle").copyTo(buildGradle, overwrite = true)

    val runner = GradleRunner.create()
        .forwardOutput()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", "compileKotlinMetadata", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
