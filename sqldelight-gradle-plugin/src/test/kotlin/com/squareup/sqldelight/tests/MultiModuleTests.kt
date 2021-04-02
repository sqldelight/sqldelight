package com.squareup.sqldelight.tests

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.androidHome
import com.squareup.sqldelight.core.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.core.SqlDelightSourceFolderImpl
import com.squareup.sqldelight.properties
import com.squareup.sqldelight.withInvariantPathSeparators
import com.squareup.sqldelight.withSortedCompilationUnits
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class MultiModuleTests {
  @Test
  fun `sqldelight dependencies are added to the compilation unit`() {
    val androidHome = androidHome()
    var fixtureRoot = File("src/test/multi-module").absoluteFile
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "--stacktrace")
      .build()

    // verify
    fixtureRoot = File(fixtureRoot, "ProjectA")
    val properties = properties(fixtureRoot)!!.databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/Database"))
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly(
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
        SqlDelightSourceFolderImpl(File(fixtureRoot, "../ProjectB/src/main/sqldelight"), true)
      )
    }
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
    var fixtureRoot = File("src/test/multi-module").absoluteFile
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "--stacktrace")
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "AndroidProject")

    val properties = properties(fixtureRoot)!!.databases.single().withInvariantPathSeparators()
      .withSortedCompilationUnits()
    assertThat(properties.packageName).isEqualTo("com.sample.android")
    assertThat(properties.outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"))
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "minApi23Debug",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibDebug/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi23/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi23Debug/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Debug/sqldelight"), false)
        )
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Release",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi23/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi23Release/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibRelease/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
        )
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Sqldelight",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi23/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi23Sqldelight/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibSqldelight/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
        )
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Debug",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibDebug/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi21/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi21Debug/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Debug/sqldelight"), false)
        )
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Release",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi21/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi21Release/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibRelease/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
        )
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Sqldelight",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi21/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibMinApi21Sqldelight/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/androidLibSqldelight/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
        )
      )
    )
  }
}
