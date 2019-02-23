package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import org.junit.Test

class CompilationUnitTests {
  @Test
  fun `JVM kotlin`() {
    withTemporaryFixture {
      gradleFile("""
        |plugins {
        |  id 'org.jetbrains.kotlin.jvm'
        |  id 'com.squareup.sqldelight'
        |}
        |
        |sqldelight {
        |  CommonDb {
        |    packageName = "com.sample"
        |  }
        |}
      """.trimMargin())

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.outputDirectory).isEqualTo("build/sqldelight/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "main",
                sourceFolders = listOf("src/main/sqldelight")
            )
        )
      }
    }
  }
  @Test
  fun `JVM kotlin with multiple databases`() {
    withTemporaryFixture {
      gradleFile("""
        |plugins {
        |  id 'org.jetbrains.kotlin.jvm'
        |  id 'com.squareup.sqldelight'
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
      """.trimMargin())

      properties().let { properties ->
        assertThat(properties.databases).containsExactly(
            SqlDelightDatabaseProperties(
                className = "CommonDb",
                packageName = "com.sample",
                outputDirectory = "build/sqldelight/CommonDb",
                compilationUnits = listOf(
                    SqlDelightCompilationUnit(
                        name = "main",
                        sourceFolders = listOf("src/main/sqldelight")
                    )
                )
            ),
            SqlDelightDatabaseProperties(
                className = "OtherDb",
                packageName = "com.sample.otherdb",
                outputDirectory = "build/sqldelight/OtherDb",
                compilationUnits = listOf(
                    SqlDelightCompilationUnit(
                        name = "main",
                        sourceFolders = listOf("src/main/sqldelight", "src/main/otherdb")
                    )
                )
            )
        )
      }
    }
  }

  @Test
  fun `Multiplatform project with multiple targets`() {
    withTemporaryFixture {
      gradleFile("""
        |plugins {
        |  id 'org.jetbrains.kotlin.multiplatform'
        |  id 'com.squareup.sqldelight'
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
        |}
      """.trimMargin())

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.outputDirectory).isEqualTo("build/sqldelight/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "jvmMain",
                sourceFolders = listOf("src/jvmMain/sqldelight", "src/commonMain/sqldelight")
            ),
            SqlDelightCompilationUnit(
                name = "jsMain",
                sourceFolders = listOf("src/jsMain/sqldelight", "src/commonMain/sqldelight")
            ),
            SqlDelightCompilationUnit(
                name = "iosArm32Main",
                sourceFolders = listOf("src/iosArm32Main/sqldelight", "src/commonMain/sqldelight")
            ),
            SqlDelightCompilationUnit(
                name = "iosArm64Main",
                sourceFolders = listOf("src/iosArm64Main/sqldelight", "src/commonMain/sqldelight")
            ),
            SqlDelightCompilationUnit(
                name = "iosX64Main",
                sourceFolders = listOf("src/iosX64Main/sqldelight", "src/commonMain/sqldelight")
            )
        )
      }
    }
  }

  @Test
  fun `Multiplatform project with android and ios targets`() {
    withTemporaryFixture {
      gradleFile("""
        |plugins {
        |  id 'org.jetbrains.kotlin.multiplatform'
        |  id 'com.android.application'
        |  id 'com.squareup.sqldelight'
        |}
        |
        |apply from: '../../../../gradle/dependencies.gradle'
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
      """.trimMargin())

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.outputDirectory).isEqualTo("build/sqldelight/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21DemoDebug",
                sourceFolders = listOf("src/androidLibMinApi21DemoDebug/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibDemo/sqldelight",
                    "src/androidLibMinApi21/sqldelight", "src/androidLibMinApi21Demo/sqldelight",
                    "src/androidLibDebug/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21DemoRelease",
                sourceFolders = listOf("src/androidLibMinApi21DemoRelease/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibDemo/sqldelight",
                    "src/androidLibMinApi21/sqldelight", "src/androidLibMinApi21Demo/sqldelight",
                    "src/androidLibRelease/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21DemoSqldelight",
                sourceFolders = listOf("src/androidLibMinApi21DemoSqldelight/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibDemo/sqldelight",
                    "src/androidLibMinApi21/sqldelight", "src/androidLibMinApi21Demo/sqldelight",
                    "src/androidLibSqldelight/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21FullDebug",
                sourceFolders = listOf("src/androidLibMinApi21FullDebug/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibFull/sqldelight",
                    "src/androidLibMinApi21/sqldelight", "src/androidLibMinApi21Full/sqldelight",
                    "src/androidLibDebug/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21FullRelease",
                sourceFolders = listOf("src/androidLibMinApi21FullRelease/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibFull/sqldelight",
                    "src/androidLibMinApi21/sqldelight", "src/androidLibMinApi21Full/sqldelight",
                    "src/androidLibRelease/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21FullSqldelight",
                sourceFolders = listOf("src/androidLibMinApi21FullSqldelight/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibFull/sqldelight",
                    "src/androidLibMinApi21/sqldelight", "src/androidLibMinApi21Full/sqldelight",
                    "src/androidLibSqldelight/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23DemoDebug",
                sourceFolders = listOf("src/androidLibMinApi23DemoDebug/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibDemo/sqldelight",
                    "src/androidLibMinApi23/sqldelight", "src/androidLibMinApi23Demo/sqldelight",
                    "src/androidLibDebug/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23DemoRelease",
                sourceFolders = listOf("src/androidLibMinApi23DemoRelease/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibDemo/sqldelight",
                    "src/androidLibMinApi23/sqldelight", "src/androidLibMinApi23Demo/sqldelight",
                    "src/androidLibRelease/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23DemoSqldelight",
                sourceFolders = listOf("src/androidLibMinApi23DemoSqldelight/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibDemo/sqldelight",
                    "src/androidLibMinApi23/sqldelight", "src/androidLibMinApi23Demo/sqldelight",
                    "src/androidLibSqldelight/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23FullDebug",
                sourceFolders = listOf("src/androidLibMinApi23FullDebug/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibFull/sqldelight",
                    "src/androidLibMinApi23/sqldelight", "src/androidLibMinApi23Full/sqldelight",
                    "src/androidLibDebug/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23FullRelease",
                sourceFolders = listOf("src/androidLibMinApi23FullRelease/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibFull/sqldelight",
                    "src/androidLibMinApi23/sqldelight", "src/androidLibMinApi23Full/sqldelight",
                    "src/androidLibRelease/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23FullSqldelight",
                sourceFolders = listOf("src/androidLibMinApi23FullSqldelight/sqldelight",
                    "src/androidLibMain/sqldelight", "src/androidLibFull/sqldelight",
                    "src/androidLibMinApi23/sqldelight", "src/androidLibMinApi23Full/sqldelight",
                    "src/androidLibSqldelight/sqldelight", "src/commonMain/sqldelight")),
            SqlDelightCompilationUnit(
                name = "iosX64Main",
                sourceFolders = listOf("src/iosX64Main/sqldelight", "src/commonMain/sqldelight"))
        )
      }
    }
  }

  @Test
  fun `android project with multiple flavors`() {
    withTemporaryFixture {
      gradleFile("""
        |plugins {
        |  id 'com.android.application'
        |  id 'org.jetbrains.kotlin.android'
        |  id 'com.squareup.sqldelight'
        |}
        |
        |apply from: '../../../../gradle/dependencies.gradle'
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
      """.trimMargin())

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.outputDirectory).isEqualTo("build/sqldelight/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "minApi23DemoDebug",
                sourceFolders = listOf("src/main/sqldelight", "src/demo/sqldelight",
                    "src/minApi23/sqldelight", "src/minApi23Demo/sqldelight",
                    "src/debug/sqldelight", "src/minApi23DemoDebug/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi23DemoRelease",
                sourceFolders = listOf("src/main/sqldelight", "src/demo/sqldelight",
                    "src/minApi23/sqldelight", "src/minApi23Demo/sqldelight",
                    "src/release/sqldelight", "src/minApi23DemoRelease/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi23DemoSqldelight",
                sourceFolders = listOf("src/main/sqldelight", "src/demo/sqldelight",
                    "src/minApi23/sqldelight", "src/minApi23Demo/sqldelight",
                    "src/sqldelight/sqldelight", "src/minApi23DemoSqldelight/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi23FullDebug",
                sourceFolders = listOf("src/main/sqldelight", "src/full/sqldelight",
                    "src/minApi23/sqldelight", "src/minApi23Full/sqldelight",
                    "src/debug/sqldelight", "src/minApi23FullDebug/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi23FullRelease",
                sourceFolders = listOf("src/main/sqldelight", "src/full/sqldelight",
                    "src/minApi23/sqldelight", "src/minApi23Full/sqldelight",
                    "src/release/sqldelight", "src/minApi23FullRelease/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi23FullSqldelight",
                sourceFolders = listOf("src/main/sqldelight", "src/full/sqldelight",
                    "src/minApi23/sqldelight", "src/minApi23Full/sqldelight",
                    "src/sqldelight/sqldelight", "src/minApi23FullSqldelight/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi21DemoDebug",
                sourceFolders = listOf("src/main/sqldelight", "src/demo/sqldelight",
                    "src/minApi21/sqldelight", "src/minApi21Demo/sqldelight",
                    "src/debug/sqldelight", "src/minApi21DemoDebug/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi21DemoRelease",
                sourceFolders = listOf("src/main/sqldelight", "src/demo/sqldelight",
                    "src/minApi21/sqldelight", "src/minApi21Demo/sqldelight",
                    "src/release/sqldelight", "src/minApi21DemoRelease/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi21DemoSqldelight",
                sourceFolders = listOf("src/main/sqldelight", "src/demo/sqldelight",
                    "src/minApi21/sqldelight", "src/minApi21Demo/sqldelight",
                    "src/sqldelight/sqldelight", "src/minApi21DemoSqldelight/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi21FullDebug",
                sourceFolders = listOf("src/main/sqldelight", "src/full/sqldelight",
                    "src/minApi21/sqldelight", "src/minApi21Full/sqldelight",
                    "src/debug/sqldelight", "src/minApi21FullDebug/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi21FullRelease",
                sourceFolders = listOf("src/main/sqldelight", "src/full/sqldelight",
                    "src/minApi21/sqldelight", "src/minApi21Full/sqldelight",
                    "src/release/sqldelight", "src/minApi21FullRelease/sqldelight")),
            SqlDelightCompilationUnit(
                name = "minApi21FullSqldelight",
                sourceFolders = listOf("src/main/sqldelight", "src/full/sqldelight",
                    "src/minApi21/sqldelight", "src/minApi21Full/sqldelight",
                    "src/sqldelight/sqldelight", "src/minApi21FullSqldelight/sqldelight"))
            )
      }
    }
  }
}