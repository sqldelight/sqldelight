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
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class KotlinVersionsTest(val kotlinVersion: String) {
  companion object {
    @Parameters(name = "{0}")
    @JvmStatic
    fun kotlinVersions() = listOf(
      "1.6.21",
      "1.7.20",
      "1.8.0-Beta",
    )
  }

  @Test
  fun `integration jvm compiles successfully with different Kotlin versions`() {
    val integrationRoot = File("src/test/integration")

    val runner = GradleRunner.create()
      .forwardOutput()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "compileKotlin", "--stacktrace", "-PoverwriteKotlinVersion=$kotlinVersion")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  @Category(Instrumentation::class)
  fun `kotlin mpp compiles successfully with different Kotlin versions`() {
    val integrationRoot = File("src/test/kotlin-mpp")

    val runner = GradleRunner.create()
      .forwardOutput()
      .withCommonConfiguration(integrationRoot)
      .withArguments("clean", "assemble", "--stacktrace", "-PoverwriteKotlinVersion=$kotlinVersion")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
