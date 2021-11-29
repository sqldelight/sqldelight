package com.squareup.sqldelight.tests

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.withCommonConfiguration
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PluginTest {
  @Test
  fun `Applying the plugin without Kotlin applied throws`() {
    val result = GradleRunner.create()
      .withCommonConfiguration(File("src/test/no-kotlin"))
      .withArguments("build", "--stacktrace")
      .buildAndFail()
    assertThat(result.output)
      .contains(
        "SQL Delight Gradle plugin applied in project ':' but no supported Kotlin plugin was found"
      )
  }

  @Test
  fun `Applying the plugin without Kotlin applied throws for Android`() {
    val result = GradleRunner.create()
      .withCommonConfiguration(File("src/test/no-kotlin-android"))
      .withArguments("build", "--stacktrace")
      .buildAndFail()
    assertThat(result.output)
      .contains(
        "SQL Delight Gradle plugin applied in project ':' but no supported Kotlin plugin was found"
      )
  }

  @Test
  fun `Applying the android plugin works fine for library projects`() {
    val result = GradleRunner.create()
      .withCommonConfiguration(File("src/test/library-project"))
      .withArguments("clean", "generateDebugDatabaseInterface", "--stacktrace")
      .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `Applying the plugin works fine for multiplatform projects`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/kotlin-mpp"))

    val result = runner
      .withArguments("clean", "generateCommonMainDatabaseInterface", "--stacktrace")
      .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")

    // Assert the plugin added the common dependency
    val dependenciesResult = runner
      .withArguments("dependencies", "--stacktrace")
      .build()
    assertThat(dependenciesResult.output).contains("app.cash.sqldelight:runtime")
  }

  @Test
  fun `The generate task is a dependency of multiplatform js target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val buildDir = File(fixtureRoot, "build/generated/sqldelight")
    buildDir.delete()

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "compileKotlinJs", "--stacktrace")
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `The generate task is a dependency of multiplatform jvm target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val buildDir = File(fixtureRoot, "build/generated/sqldelight")
    buildDir.delete()

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "compileKotlinJvm", "--stacktrace")
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun someTest() {
    val fixtureRoot = File("src/test/kotlin-mpp-configure-on-demand")
    val buildDir = File(fixtureRoot, "build/generated/sqldelight")
    buildDir.delete()

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .also {
        File(fixtureRoot, "gradle.properties").appendText("\norg.gradle.configureondemand=true")
      }
      .forwardOutput()
      .withArguments("clean", "compileKotlinJvm", "--stacktrace")
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `The generate task is a dependency of multiplatform ios target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val buildDir = File(fixtureRoot, "build/generated/sqldelight")
    buildDir.delete()

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
    var result = runner
      .withArguments("clean", "compileKotlinIosArm64", "--stacktrace")
      .forwardOutput()
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()

    buildDir.delete()
    result = runner
      .withArguments("clean", "compileKotlinIosX64", "--stacktrace")
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `The generate task is a dependency of multiplatform ios target with 1-3-20 DSL`() {
    val fixtureRoot = File("src/test/kotlin-mpp-1.3.20")
    val buildDir = File(fixtureRoot, "build/generated/sqldelight")
    buildDir.delete()

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "compileKotlinIos", "--stacktrace")
      .forwardOutput()
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `The generate task is a dependency of multiplatform link ios task`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val buildDir = File(fixtureRoot, "build/generated/sqldelight")
    buildDir.delete()

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
    var result = runner
      .withArguments("clean", "linkDebugFrameworkIosArm64", "--stacktrace")
      .forwardOutput()
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()

    buildDir.delete()
    result = runner
      .withArguments("clean", "linkDebugFrameworkIosX64", "--stacktrace")
      .build()
    assertThat(result.output).contains("generateCommonMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `the old sqldelight build folder is deleted`() {
    val fixtureRoot = File("src/test/library-project")
    val outputFolder = File(fixtureRoot, "build/generated/sqldelight").apply { mkdirs() }
    val garbage = File(outputFolder, "sup.txt").apply { createNewFile() }

    assertThat(garbage.exists()).isTrue()

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateDebugDatabaseInterface", "--stacktrace")
      .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")

    assertThat(garbage.exists()).isFalse()
  }

  @Test
  fun `Applying the plugin without a sqldelight block warns`() {
    val result = GradleRunner.create()
      .withCommonConfiguration(File("src/test/no-sqldelight"))
      .withArguments("build", "--stacktrace")
      .build()
    assertThat(result.output)
      .contains("SQLDelight Gradle plugin was applied but there are no databases set up.")
  }
}
