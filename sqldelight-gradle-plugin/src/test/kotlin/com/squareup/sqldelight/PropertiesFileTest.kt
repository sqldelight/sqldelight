package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.SqlDelightSourceFolder
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class PropertiesFileTest {
  @Test fun `properties file generates correctly`() {
    val fixtureRoot = File("src/test/properties-file")
    File(fixtureRoot, ".idea").mkdir()

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectory).isEqualTo("build/generated/sqldelight/code/Database")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly(
          SqlDelightSourceFolder("src/main/sqldelight", false))
    }

    propertiesFile.delete()
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
          SqlDelightCompilationUnit(
              name = "androidLibDebug",
              sourceFolders = listOf(
                  SqlDelightSourceFolder(path = "src/androidLibDebug/sqldelight", dependency = false),
                  SqlDelightSourceFolder(path = "src/androidLibMain/sqldelight", dependency = false),
                  SqlDelightSourceFolder(path = "src/commonMain/sqldelight", dependency = false)
              )
          ),
          SqlDelightCompilationUnit(
              name = "androidLibRelease",
              sourceFolders = listOf(
                  SqlDelightSourceFolder(path = "src/androidLibMain/sqldelight", dependency = false),
                  SqlDelightSourceFolder(path = "src/androidLibRelease/sqldelight", dependency = false),
                  SqlDelightSourceFolder(path = "src/commonMain/sqldelight", dependency = false)
              )
          ),
          SqlDelightCompilationUnit(
              name = "metadataMain",
              sourceFolders = listOf(
                  SqlDelightSourceFolder(path = "src/commonMain/sqldelight", dependency = false)
              )
          )
      )
    }
  }
}
