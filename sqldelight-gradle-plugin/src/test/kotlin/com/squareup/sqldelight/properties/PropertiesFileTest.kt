package com.squareup.sqldelight.properties

import app.cash.sqldelight.core.dialectPreset
import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.gradle.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.gradle.SqlDelightSourceFolderImpl
import com.squareup.sqldelight.properties
import com.squareup.sqldelight.withCommonConfiguration
import com.squareup.sqldelight.withInvariantPathSeparators
import com.squareup.sqldelight.withTemporaryFixture
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PropertiesFileTest {
  @Test fun `properties file generates correctly`() {
    val fixtureRoot = File("src/test/properties-file").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
      .build()

    // verify
    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/Database"))
      assertThat(sourceFolders).containsExactly(
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false)
      )
    }
  }

  @Test fun `correct default dialect and package name for android`() {
    val fixtureRoot = File("src/test/properties-file-android").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateDebugDatabaseInterface", "--stacktrace")
      .build()

    // verify
    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.dialectPreset).isEqualTo(DialectPreset.SQLITE_3_25)
  }

  @Test fun `properties file for an android multiplatform module`() {
    withTemporaryFixture {
      gradleFile(
        """|
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.multiplatform'
        |apply plugin: 'app.cash.sqldelight'
        |apply plugin: 'com.android.library'
        |
        |repositories {
        |  maven {
        |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |  }
        |}
        |
        |archivesBaseName = 'Test'
        |
        |android {
        |  compileSdkVersion versions.compileSdk
        |
        |  defaultConfig {
        |    minSdkVersion versions.minSdk
        |  }
        |}
        |
        |sqldelight {
        |  CashDatabase {
        |    packageName = "com.squareup.sqldelight.sample"
        |  }
        |}
        |
        |kotlin {
        |  sourceSets {
        |    androidLibMain {
        |    }
        |  }
        |
        |  targetFromPreset(presets.android, 'androidLib')
        |}
      """.trimMargin()
      )

      val database = properties().databases.single()
      assertThat(database.compilationUnits).containsExactly(
        SqlDelightCompilationUnitImpl(
          name = "commonMain",
          sourceFolders = listOf(
            SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), dependency = false)
          ),
          outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CashDatabase"),
        )
      )
    }
  }
}
