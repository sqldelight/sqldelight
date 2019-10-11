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
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

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
        .withArguments("clean", "generateDebugDatabaseInterface", "--stacktrace")
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
        .withArguments("clean", "generateJvmMainDatabaseInterface", "--stacktrace")
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

    val buildDir = File(fixtureRoot, sqldelightDir)

    buildDir.delete()
    val result = runner
        .withArguments("clean", "compileKotlinJs", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateJsMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun `The generate task is a dependency of multiplatform jvm target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, sqldelightDir)

    buildDir.delete()
    val result = runner
        .withArguments("clean", "compileKotlinJvm", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateJvmMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  fun someTest() {
    val fixtureRoot = File("src/test/kotlin-mpp-configure-on-demand")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .forwardOutput()

    val buildDir = File(fixtureRoot, sqldelightDir)

    buildDir.delete()
    val result = runner
        .withArguments("clean", "compileKotlinJvm", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateJvmMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  @Category(IosTest::class)
  fun `The generate task is a dependency of multiplatform ios target`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, sqldelightDir)

    buildDir.delete()
    var result = runner
        .withArguments("clean", "compileKotlinIosArm64", "--stacktrace")
        .forwardOutput()
        .build()
    assertThat(result.output).contains("generateIosArm64MainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()

    buildDir.delete()
    result = runner
        .withArguments("clean", "compileKotlinIosX64", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateIosX64MainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  @Category(IosTest::class)
  fun `The generate task is a dependency of multiplatform ios target with 1-3-20 DSL`() {
    val fixtureRoot = File("src/test/kotlin-mpp-1.3.20")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, sqldelightDir)

    buildDir.delete()
    val result = runner
        .withArguments("clean", "compileKotlinIos", "--stacktrace")
        .forwardOutput()
        .build()
    assertThat(result.output).contains("generateIosMainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()
  }

  @Test
  @Category(IosTest::class)
  fun `The generate task is a dependency of multiplatform link ios task`() {
    val fixtureRoot = File("src/test/kotlin-mpp")
    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val buildDir = File(fixtureRoot, sqldelightDir)

    buildDir.delete()
    var result = runner
        .withArguments("clean", "linkDebugFrameworkIosArm64", "--stacktrace")
        .forwardOutput()
        .build()
    assertThat(result.output).contains("generateIosArm64MainDatabaseInterface")
    assertThat(buildDir.exists()).isTrue()

    buildDir.delete()
    result = runner
        .withArguments("clean", "linkDebugFrameworkIosX64", "--stacktrace")
        .build()
    assertThat(result.output).contains("generateIosX64MainDatabaseInterface")
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

    val outputFolder = File(fixtureRoot, sqldelightDir).apply { mkdirs() }
    val garbage = File(outputFolder, "sup.txt").apply { createNewFile() }

    assertThat(garbage.exists()).isTrue()

    val result = runner
        .withArguments("clean", "generateDebugDatabaseInterface", "--stacktrace")
        .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")

    assertThat(garbage.exists()).isFalse()
  }
}
