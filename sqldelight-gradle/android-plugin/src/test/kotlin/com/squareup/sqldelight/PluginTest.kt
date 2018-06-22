package com.squareup.sqldelight

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PluginTest {
  @Test
  fun `Applying the android plugin on kotlin plugin throws`() {
    val fixtureRoot = File("src/test/kotlin-project")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("build", "--stacktrace", "-Dsqldelight.skip.runtime=true")
        .buildAndFail()
    Truth.assertThat(result.output)
        .contains("""
      Kotlin projects need to apply the sqldelight kotlin plugin:

      buildscript {
        dependencies {
          classpath "com.squareup.sqldelight:gradle-plugin:$VERSION
        }
      }

      apply plugin: "com.squareup.sqldelight"
      """.trimIndent())
  }

  @Test
  fun `Applying the android plugin works fine for library projects`() {
    val fixtureRoot = File("src/test/library-project")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("clean", "generateDebugSqlDelightInterface", "--stacktrace",
            "-Dsqldelight.skip.runtime=true")
        .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}