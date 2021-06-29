package com.squareup.sqldelight.properties

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.androidHome
import com.squareup.sqldelight.gradle.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.gradle.SqlDelightSourceFolderImpl
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
      .setDebug(true) // Run in-process.
      .build()

    // verify
    fixtureRoot = File(fixtureRoot, "ProjectA")
    val properties = properties(fixtureRoot)!!.databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/Database"))
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
      .setDebug(true) // Run in-process.

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
      .setDebug(true) // Run in-process.

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
      .setDebug(true) // Run in-process.
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "AndroidProject")

    val properties = properties(fixtureRoot)!!.databases.single().withInvariantPathSeparators()
      .withSortedCompilationUnits()
    assertThat(properties.packageName).isEqualTo("com.sample.android")
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "minApi23Debug",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Debug/sqldelight"), false)
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Debug"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Release",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Release"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Sqldelight",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Sqldelight"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Debug",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Debug/sqldelight"), false)
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Debug"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Release",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Release"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Sqldelight",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Sqldelight"),
      )
    )
  }

  @Test
  fun `diamond dependency is correctly resolved`() {
    var fixtureRoot = File("src/test/diamond-dependency").absoluteFile

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "--stacktrace")
      .setDebug(true) // Run in-process.
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "app")

    val properties = properties(fixtureRoot)!!.databases.single().withInvariantPathSeparators()
      .withSortedCompilationUnits()
    assertThat(properties.packageName).isEqualTo("com.example.app")
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "main",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../bottom/src/main/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../middleA/src/main/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../middleB/src/main/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/Database"),
      )
    )
  }
}
