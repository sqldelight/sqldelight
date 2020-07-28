package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.SqlDelightSourceFolder
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class MultiModuleTests {
  @Test
  fun `sqldelight dependencies are added to the compilation unit`() {
    val androidHome = androidHome()
    val fixtureRoot = File("src/test/multi-module")
    File(fixtureRoot, ".idea").mkdir()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "--stacktrace")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/ProjectA/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectory).isEqualTo("build/generated/sqldelight/code/Database")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly(SqlDelightSourceFolder("src/main/sqldelight", false), SqlDelightSourceFolder("../ProjectB/src/main/sqldelight", true))
    }

    propertiesFile.delete()
  }

  @Test
  fun integrationTests() {
    val androidHome = androidHome()
    val integrationRoot = File("src/test/multi-module")
    File(integrationRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withArguments("clean", ":ProjectA:check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `android multi module integration tests`() {
    val androidHome = androidHome()
    val integrationRoot = File("src/test/multi-module")
    File(integrationRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withArguments("clean", ":AndroidProject:connectedCheck", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `the android target of a multiplatform module is a dependency for an android only module`() {
    val androidHome = androidHome()
    val fixtureRoot = File("src/test/multi-module")
    File(fixtureRoot, ".idea").mkdir()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "--stacktrace")
        .forwardOutput()
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/AndroidProject/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single()
        .withInvariantPathSeparators()
        .withSortedCompilationUnits()
    assertThat(properties.packageName).isEqualTo("com.sample.android")
    assertThat(properties.outputDirectory).isEqualTo("build/generated/sqldelight/code/CommonDb")
    assertThat(properties.compilationUnits).containsExactly(
        SqlDelightCompilationUnit(
            name = "minApi23Debug",
            sourceFolders = listOf(
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Debug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true),
                SqlDelightSourceFolder("src/debug/sqldelight", false),
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Debug/sqldelight", false))),
        SqlDelightCompilationUnit(
            name = "minApi23Release",
            sourceFolders = listOf(
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Release/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true),
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Release/sqldelight", false),
                SqlDelightSourceFolder("src/release/sqldelight", false))),
        SqlDelightCompilationUnit(
            name = "minApi23Sqldelight",
            sourceFolders = listOf(
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Sqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true),
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Sqldelight/sqldelight", false),
                SqlDelightSourceFolder("src/sqldelight/sqldelight", false))),
        SqlDelightCompilationUnit(
            name = "minApi21Debug",
            sourceFolders = listOf(
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Debug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true),
                SqlDelightSourceFolder("src/debug/sqldelight", false),
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Debug/sqldelight", false))),
        SqlDelightCompilationUnit(
            name = "minApi21Release",
            sourceFolders = listOf(
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Release/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true),
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Release/sqldelight", false),
                SqlDelightSourceFolder("src/release/sqldelight", false))),
        SqlDelightCompilationUnit(
            name = "minApi21Sqldelight",
            sourceFolders = listOf(
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Sqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true),
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Sqldelight/sqldelight", false),
                SqlDelightSourceFolder("src/sqldelight/sqldelight", false)))
    )

    propertiesFile.delete()
  }
}
