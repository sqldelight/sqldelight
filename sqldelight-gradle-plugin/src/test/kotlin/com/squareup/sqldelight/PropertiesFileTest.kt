package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.core.SqlDelightSourceFolderImpl
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class PropertiesFileTest {
  @Test fun `properties file generates correctly`() {
    val fixtureRoot = File("src/test/properties-file").absoluteFile

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
        .build()

    // verify
    val properties = properties(fixtureRoot)!!.databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/Database"))
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false))
    }
  }

  @Test fun `properties file for an android multiplatform module`() {
    withTemporaryFixture {
      gradleFile("""|
        |buildscript {
        |  apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |  repositories {
        |    maven {
        |      url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |    }
        |    mavenCentral()
        |    google()
        |    jcenter()
        |  }
        |  dependencies {
        |    classpath 'com.squareup.sqldelight:gradle-plugin:+'
        |    classpath deps.plugins.kotlin
        |    classpath deps.plugins.android
        |  }
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.multiplatform'
        |apply plugin: 'com.squareup.sqldelight'
        |apply plugin: 'com.android.library'
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
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
        |  compileOptions {
        |    sourceCompatibility JavaVersion.VERSION_1_8
        |    targetCompatibility JavaVersion.VERSION_1_8
        |  }
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
      """.trimMargin())

      val database = properties().databases.single()
      assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
              name = "androidLibDebug",
              sourceFolders = listOf(
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDebug/sqldelight"), dependency = false),
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), dependency = false),
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), dependency = false)
              )
          ),
          SqlDelightCompilationUnitImpl(
              name = "androidLibRelease",
              sourceFolders = listOf(
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), dependency = false),
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibRelease/sqldelight"), dependency = false),
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), dependency = false)
              )
          ),
          SqlDelightCompilationUnitImpl(
              name = "metadataMain",
              sourceFolders = listOf(
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), dependency = false)
              )
          )
      )
    }
  }
}
