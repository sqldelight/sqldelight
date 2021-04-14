package com.squareup.sqldelight.tests

import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.gradle.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import com.squareup.sqldelight.gradle.SqlDelightSourceFolderImpl
import com.squareup.sqldelight.withTemporaryFixture
import org.junit.Test
import java.io.File

class CompilationUnitTests {
  @Test
  fun `JVM kotlin`() {
    withTemporaryFixture {
      gradleFile(
        """
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.jvm'
        |apply plugin: 'com.squareup.sqldelight'
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |repositories {
        |  maven {
        |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |  }
        |}
        |
        |sqldelight {
        |  CommonDb {
        |    packageName = "com.sample"
        |  }
        |}
      """.trimMargin()
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "main",
            sourceFolders = listOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false)),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          )
        )
      }
    }
  }

  @Test
  fun `JVM kotlin with multiple databases`() {
    withTemporaryFixture {
      gradleFile(
        """
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.jvm'
        |apply plugin: 'com.squareup.sqldelight'
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |repositories {
        |  maven {
        |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |  }
        |}
        |
        |sqldelight {
        |  CommonDb {
        |    packageName = "com.sample"
        |  }
        |
        |  OtherDb {
        |    packageName = "com.sample.otherdb"
        |    sourceFolders = ["sqldelight", "otherdb"]
        |  }
        |}
      """.trimMargin()
      )

      properties().let { properties ->
        assertThat(properties.databases).containsExactly(
          SqlDelightDatabasePropertiesImpl(
            className = "CommonDb",
            packageName = "com.sample",
            compilationUnits = listOf(
              SqlDelightCompilationUnitImpl(
                name = "main",
                sourceFolders = listOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false)),
                outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
              )
            ),
            dependencies = emptyList(),
            dialectPresetName = DialectPreset.SQLITE_3_18.name,
            rootDirectory = fixtureRoot
          ),
          SqlDelightDatabasePropertiesImpl(
            className = "OtherDb",
            packageName = "com.sample.otherdb",
            compilationUnits = listOf(
              SqlDelightCompilationUnitImpl(
                name = "main",
                sourceFolders = listOf(
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/otherdb"), false),
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
                ),
                outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/OtherDb"),
              )
            ),
            dependencies = emptyList(),
            dialectPresetName = DialectPreset.SQLITE_3_18.name,
            rootDirectory = fixtureRoot
          )
        )
      }
    }
  }

  @Test
  fun `Multiplatform project with multiple targets`() {
    withTemporaryFixture {
      gradleFile(
        """
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.multiplatform'
        |apply plugin: 'com.squareup.sqldelight'
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |repositories {
        |  maven {
        |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |  }
        |}
        |
        |sqldelight {
        |  CommonDb {
        |    packageName = "com.sample"
        |  }
        |}
        |
        |kotlin {
        |  targetFromPreset(presets.jvm, 'jvm')
        |  targetFromPreset(presets.js, 'js')
        |  targetFromPreset(presets.iosArm32, 'iosArm32')
        |  targetFromPreset(presets.iosArm64, 'iosArm64')
        |  targetFromPreset(presets.iosX64, 'iosX64')
        |  targetFromPreset(presets.macosX64, 'macosX64')
        |}
      """.trimMargin()
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "jvmMain",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/jvmMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          ),
          SqlDelightCompilationUnitImpl(
            name = "jsMain",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/jsMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          ),
          SqlDelightCompilationUnitImpl(
            name = "iosArm32Main",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/iosArm32Main/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          ),
          SqlDelightCompilationUnitImpl(
            name = "iosArm64Main",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/iosArm64Main/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          ),
          SqlDelightCompilationUnitImpl(
            name = "iosX64Main",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/iosX64Main/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          ),
          SqlDelightCompilationUnitImpl(
            name = "macosX64Main",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/macosX64Main/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          ),
          SqlDelightCompilationUnitImpl(
            name = "metadataMain",
            sourceFolders = listOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb")
          )
        )
      }
    }
  }

  @Test
  fun `Multiplatform project with android and ios targets`() {
    withTemporaryFixture {
      gradleFile(
        """
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.multiplatform'
        |apply plugin: 'com.android.application'
        |apply plugin: 'com.squareup.sqldelight'
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |repositories {
        |  maven {
        |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |  }
        |}
        |
        |
        |sqldelight {
        |  CommonDb {
        |    packageName = "com.sample"
        |  }
        |}
        |
        |android {
        |  compileSdkVersion versions.compileSdk
        |
        |  buildTypes {
        |    release {}
        |    sqldelight {}
        |  }
        |
        |  flavorDimensions "api", "mode"
        |
        |  productFlavors {
        |    demo {
        |      applicationIdSuffix ".demo"
        |      dimension "mode"
        |    }
        |    full {
        |      applicationIdSuffix ".full"
        |      dimension "mode"
        |    }
        |    minApi21 {
        |      dimension "api"
        |    }
        |    minApi23 {
        |      dimension "api"
        |    }
        |  }
        |}
        |
        |kotlin {
        |  targetFromPreset(presets.iosX64, 'iosX64')
        |  targetFromPreset(presets.android, 'androidLib')
        |}
      """.trimMargin()
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi21DemoDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDemo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21DemoDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi21DemoRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDemo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21DemoRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi21DemoSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDemo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(
                File(fixtureRoot, "src/androidLibMinApi21DemoSqldelight/sqldelight"),
                false
              ),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi21FullDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibFull/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21FullDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi21FullRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibFull/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21FullRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi21FullSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibFull/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(
                File(fixtureRoot, "src/androidLibMinApi21FullSqldelight/sqldelight"),
                false
              ),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi23DemoDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDemo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23DemoDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi23DemoRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDemo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23DemoRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi23DemoSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDemo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(
                File(fixtureRoot, "src/androidLibMinApi23DemoSqldelight/sqldelight"),
                false
              ),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi23FullDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibFull/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23FullDebug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi23FullRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibFull/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23FullRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "androidLibMinApi23FullSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibFull/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibMinApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(
                File(fixtureRoot, "src/androidLibMinApi23FullSqldelight/sqldelight"),
                false
              ),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/androidLibSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "iosX64Main",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/iosX64Main/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "metadataMain",
            sourceFolders = listOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          )
        )
      }
    }
  }

  @Test
  fun `android project with multiple flavors`() {
    withTemporaryFixture {
      gradleFile(
        """
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'com.android.application'
        |apply plugin: 'org.jetbrains.kotlin.android'
        |apply plugin: 'com.squareup.sqldelight'
        |apply from: "${"$"}{rootDir}/../../../../gradle/dependencies.gradle"
        |
        |repositories {
        |  maven {
        |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
        |  }
        |}
        |
        |sqldelight {
        |  CommonDb {
        |    packageName = "com.sample"
        |  }
        |}
        |
        |android {
        |  compileSdkVersion versions.compileSdk
        |
        |  buildTypes {
        |    release {}
        |    sqldelight {}
        |  }
        |
        |  flavorDimensions "api", "mode"
        |
        |  productFlavors {
        |    demo {
        |      applicationIdSuffix ".demo"
        |      dimension "mode"
        |    }
        |    full {
        |      applicationIdSuffix ".full"
        |      dimension "mode"
        |    }
        |    minApi21 {
        |      dimension "api"
        |    }
        |    minApi23 {
        |      dimension "api"
        |    }
        |  }
        |}
      """.trimMargin()
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "minApi23DemoDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23DemoDebug/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23DemoRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23DemoRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23DemoSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23DemoSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23FullDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23FullDebug/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23FullRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23FullRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23FullSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23FullSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21DemoDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21DemoDebug/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21DemoRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21DemoRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21DemoSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21DemoSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21FullDebug",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21FullDebug/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21FullRelease",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21FullRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21FullSqldelight",
            sourceFolders = listOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21FullSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false)
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb"),
          )
        )
      }
    }
  }
}
