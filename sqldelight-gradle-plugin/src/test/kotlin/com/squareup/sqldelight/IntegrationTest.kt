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
}
