package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PluginTest {
  @Test
  fun `Applying the plugin without Kotlin applied throws`() {
    val fixtureRoot = File("src/test/no-kotlin")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("build", "--stacktrace", "-Dsqldelight.skip.runtime=true")
        .buildAndFail()
    assertThat(result.output)
        .contains("SQL Delight Gradle plugin applied in project ':' but no supported Kotlin plugin was found")
  }
  @Test
  fun `Applying the plugin without Kotlin applied throws for Android`() {
    val fixtureRoot = File("src/test/no-kotlin-android")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("build", "--stacktrace", "-Dsqldelight.skip.runtime=true")
        .buildAndFail()
    assertThat(result.output)
        .contains("SQL Delight Gradle plugin applied in project ':' but no supported Kotlin plugin was found")
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
