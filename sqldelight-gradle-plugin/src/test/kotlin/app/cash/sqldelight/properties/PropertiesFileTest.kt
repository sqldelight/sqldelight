package app.cash.sqldelight.properties

import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import app.cash.sqldelight.properties
import app.cash.sqldelight.withCommonConfiguration
import app.cash.sqldelight.withInvariantPathSeparators
import app.cash.sqldelight.withTemporaryFixture
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

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
      assertThat(outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/Database/main"))
      assertThat(sourceFolders).containsExactly(
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
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
  }

  @Test fun `properties file for an android multiplatform module`() {
    withTemporaryFixture {
      gradleFile(
        """|
        |plugins {
        |  alias(libs.plugins.kotlin.multiplatform)
        |  alias(libs.plugins.sqldelight)
        |  alias(libs.plugins.android.library)
        |}
        |
        |archivesBaseName = 'Test'
        |
        |android {
        |  namespace 'com.example.namespace'
        |  compileSdk libs.versions.compileSdk.get() as int
        |
        |  defaultConfig {
        |    minSdk libs.versions.minSdk.get() as int
        |  }
        |}
        |
        |sqldelight {
        |  databases {
        |    CashDatabase {
        |      packageName = "app.cash.sqldelight.sample"
        |    }
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
        """.trimMargin(),
      )

      val database = properties().databases.single()
      assertThat(database.compilationUnits).containsExactly(
        SqlDelightCompilationUnitImpl(
          name = "commonMain",
          sourceFolders = listOf(
            SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), dependency = false),
          ),
          outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CashDatabase/commonMain"),
        ),
      )
    }
  }
}
