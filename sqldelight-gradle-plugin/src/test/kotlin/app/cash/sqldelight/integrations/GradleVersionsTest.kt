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
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class GradleVersionsTest(private val gradleVersion: String) {
  companion object {
    @Parameters(name = "{0}")
    @JvmStatic
    fun kotlinVersions() = listOf(
      // MIN_GRADLE_VERSION,
      // We use version catalogs in tests too but this feature is only stable since 7.4.
      // Test MIN_GRADLE_VERSION too if MIN_GRADLE_VERSION is higher than 7.4.
      "7.4.2",
      "7.6.1",
      "8.0.2",
    )
  }

  @Test
  fun `integration jvm compiles successfully with different Gradle versions`() {
    val integrationRoot = File("src/test/integration")

    val runner = GradleRunner.create()
      .forwardOutput()
      .withGradleVersion(gradleVersion)
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "compileKotlin", "--stacktrace").apply {
        // Don't cache all Gradle versions on CI, this will break GH actions size limit.
        if (System.getenv("CI") == "true") {
          val tmp = Files.createTempDirectory("gradleVersionTest")
          withArguments(arguments + "-Dgradle.user.home=$tmp")
        }
      }

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
