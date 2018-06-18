package com.squareup.sqldelight

import com.google.common.truth.Truth
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PluginTest {
  @Test
  fun `Applying the android plugin without AGP applied throws`() {
    val fixtureRoot = File("src/test/android-project")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("build", "--stacktrace", "-Dsqldelight.skip.runtime=true")
        .buildAndFail()
    Truth.assertThat(result.output)
        .contains("""
      Android projects need to apply the sqldelight android plugin:

      buildscript {
        dependencies {
          classpath "com.squareup.sqldelight:android-gradle-plugin:$VERSION
        }
      }

      apply plugin: "com.squareup.sqldelight.android"
      """.trimIndent())
  }
}