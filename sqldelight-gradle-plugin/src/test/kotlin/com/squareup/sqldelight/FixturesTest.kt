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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Properties

@RunWith(Parameterized::class)
class FixturesTest {
  @Parameter(0)
  @JvmField var fixtureRoot: File? = null

  @Parameter(1)
  @JvmField var name: String? = null

  var removeGradleAfter = false
  var removeManifestAfter = false

  @Before
  fun before() {
    if (!File(fixtureRoot, "build.gradle").exists()) {
      removeGradleAfter = true
      Files.copy(File(fixtureRoot, "../build.gradle").toPath(),
          File(fixtureRoot, "build.gradle").toPath())
    }

    if (!File(fixtureRoot, "src/main/AndroidManifest.xml").exists()) {
      removeManifestAfter = true
      Files.copy(File(fixtureRoot, "../AndroidManifest.xml").toPath(),
          File(fixtureRoot, "src/main/AndroidManifest.xml").toPath())
    }
  }

  @After
  fun after() {
    if (removeGradleAfter) {
      File(fixtureRoot, "build.gradle").delete()
    }
    if (removeManifestAfter) {
      File(fixtureRoot, "src/main/AndroidManifest.xml").delete()
    }
  }

  @Test fun execute() {
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "build", "--stacktrace", "-Dsqldelight.skip.runtime=true")

    if (File(fixtureRoot, "ignored.txt").exists()) {
      println("Skipping ignored test $name.")
      return
    }

    val expectedFailure = File(fixtureRoot, "failure.txt")
    if (expectedFailure.exists()) {
      val result = runner.buildAndFail()
      for (chunk in expectedFailure.readText().split("\n\n")) {
        assertThat(result.output).contains(chunk)
      }
    } else {
      val result = runner.build()
      assertThat(result.output).contains("BUILD SUCCESSFUL")

      val expectedDir = File(fixtureRoot, "expected/").toPath()
      val outputDir = File(fixtureRoot, "build/generated/source/sqldelight/").toPath()
      Files.walkFileTree(expectedDir, object : SimpleFileVisitor<Path>() {
        override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
          val relative = expectedDir.relativize(expectedFile).toString()
          val actualFile = outputDir.resolve(relative)
          if (!Files.exists(actualFile)) {
            throw AssertionError("Expected file not found: $actualFile")
          }

          val expected = String(Files.readAllBytes(expectedFile), StandardCharsets.UTF_8)
          val actual = String(Files.readAllBytes(actualFile), StandardCharsets.UTF_8)
          assertThat(actual).named(relative).isEqualTo(expected)

          return CONTINUE
        }
      })
    }
  }

  private fun androidHome(): String {
    val env = System.getenv("ANDROID_HOME")
    if (env != null) {
      return env
    }
    val localProp = File(File(System.getProperty("user.dir")).parentFile, "local.properties")
    if (localProp.exists()) {
      val prop = Properties()
      localProp.inputStream().use {
        prop.load(it)
      }
      val sdkHome = prop.getProperty("sdk.dir")
      if (sdkHome != null) {
        return sdkHome
      }
    }
    throw IllegalStateException(
        "Missing 'ANDROID_HOME' environment variable or local.properties with 'sdk.dir'")
  }

  companion object {
    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{1}")
    @JvmStatic fun parameters() = File("src/test/fixtures").listFiles()
        .filter { it.isDirectory }
        .map { arrayOf(it, it.name) }
  }
}
