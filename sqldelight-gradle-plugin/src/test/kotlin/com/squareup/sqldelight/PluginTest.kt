package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import org.junit.experimental.categories.Category
import java.io.File

class PluginTest {
  @Test
  fun `Applying the plugin without Kotlin applied throws`() {
    val fixtureRoot = File("src/test/no-kotlin")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("build", "--stacktrace")
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
        .withArguments("build", "--stacktrace")
        .buildAndFail()
    assertThat(result.output)
        .contains("SQL Delight Gradle plugin applied in project ':' but no supported Kotlin plugin was found")
  }

  @Test
  fun `Applying the android plugin works fine for library projects`() {
    val androidHome = androidHome()
    val fixtureRoot = File("src/test/library-project")
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("clean", "generateDebugSqlDelightInterface", "--stacktrace")
        .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `Applying the plugin works fine for multiplatform projects`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("clean", "generateSqlDelightInterface", "--stacktrace")
        .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")

    // Assert the plugin added the common dependency
    val dependenciesResult = runner
        .withArguments("dependencies", "--stacktrace")
        .build()
    assertThat(dependenciesResult.output).contains("com.squareup.sqldelight:runtime")
  }

  @Test
  fun `The generate task is a dependency of multiplatform js target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, "build/sqldelight")

    buildDir.delete()
    val result = runner
        .withArguments("clean", "compileKotlinJs", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateSqlDelightInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `The generate task is a dependency of multiplatform jvm target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, "build/sqldelight")

    buildDir.delete()
    val result = runner
        .withArguments("clean", "compileKotlinJvm", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateSqlDelightInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  @Category(IosTest::class)
  fun `The generate task is a dependency of multiplatform ios target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, "build/sqldelight")

    buildDir.delete()
    var result = runner
        .withArguments("clean", "linkIosArm64", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateSqlDelightInterface")
    assertThat(buildDir.exists()).isTrue()

    buildDir.delete()
    result = runner
        .withArguments("clean", "linkIosX64", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateSqlDelightInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `the old sqldelight build folder is deleted`() {
    val androidHome = androidHome()
    val fixtureRoot = File("src/test/library-project")
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val outputFolder = File(fixtureRoot, "build/sqldelight").apply { mkdirs() }
    val garbage = File(outputFolder, "sup.txt").apply { createNewFile() }

    assertThat(garbage.exists()).isTrue()

    val result = runner
        .withArguments("clean", "generateDebugSqlDelightInterface", "--stacktrace")
        .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")

    assertThat(garbage.exists()).isFalse()
  }
}
