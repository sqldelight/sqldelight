package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightSourceFolder
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
        assertThat(database.outputDirectory).isEqualTo("build/generated/sqldelight/code/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "main",
                sourceFolders = listOf(SqlDelightSourceFolder("src/main/sqldelight", false))
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
                outputDirectory = "build/generated/sqldelight/code/CommonDb",
                compilationUnits = listOf(
                    SqlDelightCompilationUnit(
                        name = "main",
                        sourceFolders = listOf(SqlDelightSourceFolder("src/main/sqldelight", false))
                    )
                ),
                dependencies = emptyList()
            ),
            SqlDelightDatabaseProperties(
                className = "OtherDb",
                packageName = "com.sample.otherdb",
                outputDirectory = "build/generated/sqldelight/code/OtherDb",
                compilationUnits = listOf(
                    SqlDelightCompilationUnit(
                        name = "main",
                        sourceFolders = listOf(
                            SqlDelightSourceFolder("src/main/otherdb", false),
                            SqlDelightSourceFolder("src/main/sqldelight", false)
                        )
                    )
                ),
                dependencies = emptyList()
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
        assertThat(database.outputDirectory).isEqualTo("build/generated/sqldelight/code/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "jvmMain",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false),
                    SqlDelightSourceFolder("src/jvmMain/sqldelight", false)
                )
            ),
            SqlDelightCompilationUnit(
                name = "jsMain",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false),
                    SqlDelightSourceFolder("src/jsMain/sqldelight", false)
                )
            ),
            SqlDelightCompilationUnit(
                name = "iosArm32Main",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false),
                    SqlDelightSourceFolder("src/iosArm32Main/sqldelight", false)
                )
            ),
            SqlDelightCompilationUnit(
                name = "iosArm64Main",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false),
                    SqlDelightSourceFolder("src/iosArm64Main/sqldelight", false)
                )
            ),
            SqlDelightCompilationUnit(
                name = "iosX64Main",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false),
                    SqlDelightSourceFolder("src/iosX64Main/sqldelight", false)
                )
            ),
            SqlDelightCompilationUnit(
                name = "metadataMain",
                sourceFolders = listOf(SqlDelightSourceFolder("src/commonMain/sqldelight", false))
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
        assertThat(database.outputDirectory).isEqualTo("build/generated/sqldelight/code/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21DemoDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibDemo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21DemoDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21DemoRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDemo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21DemoRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21DemoSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDemo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21DemoSqldelight/sqldelight",
                        false),
                    SqlDelightSourceFolder("src/androidLibSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21FullDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibFull/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21Full/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21FullDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21FullRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibFull/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21Full/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21FullRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi21FullSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibFull/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21Full/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi21FullSqldelight/sqldelight",
                        false),
                    SqlDelightSourceFolder("src/androidLibSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23DemoDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibDemo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23DemoDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23DemoRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDemo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23DemoRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23DemoSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDemo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23DemoSqldelight/sqldelight",
                        false),
                    SqlDelightSourceFolder("src/androidLibSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23FullDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibFull/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23Full/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23FullDebug/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23FullRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibFull/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23Full/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23FullRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "androidLibMinApi23FullSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/androidLibFull/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMain/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23Full/sqldelight", false),
                    SqlDelightSourceFolder("src/androidLibMinApi23FullSqldelight/sqldelight",
                        false),
                    SqlDelightSourceFolder("src/androidLibSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "iosX64Main",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/commonMain/sqldelight", false),
                    SqlDelightSourceFolder("src/iosX64Main/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "metadataMain",
                sourceFolders = listOf(SqlDelightSourceFolder("src/commonMain/sqldelight", false))
            )
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
        assertThat(database.outputDirectory).isEqualTo("build/generated/sqldelight/code/CommonDb")
        assertThat(database.compilationUnits).containsExactly(
            SqlDelightCompilationUnit(
                name = "minApi23DemoDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/debug/sqldelight", false),
                    SqlDelightSourceFolder("src/demo/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23DemoDebug/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi23DemoRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/demo/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23DemoRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/release/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi23DemoSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/demo/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23DemoSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/sqldelight/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi23FullDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/debug/sqldelight", false),
                    SqlDelightSourceFolder("src/full/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23Full/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23FullDebug/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi23FullRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/full/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23Full/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23FullRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/release/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi23FullSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/full/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23Full/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi23FullSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/sqldelight/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi21DemoDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/debug/sqldelight", false),
                    SqlDelightSourceFolder("src/demo/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21DemoDebug/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi21DemoRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/demo/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21DemoRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/release/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi21DemoSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/demo/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21Demo/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21DemoSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/sqldelight/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi21FullDebug",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/debug/sqldelight", false),
                    SqlDelightSourceFolder("src/full/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21Full/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21FullDebug/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi21FullRelease",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/full/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21Full/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21FullRelease/sqldelight", false),
                    SqlDelightSourceFolder("src/release/sqldelight", false)
                )),
            SqlDelightCompilationUnit(
                name = "minApi21FullSqldelight",
                sourceFolders = listOf(
                    SqlDelightSourceFolder("src/full/sqldelight", false),
                    SqlDelightSourceFolder("src/main/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21Full/sqldelight", false),
                    SqlDelightSourceFolder("src/minApi21FullSqldelight/sqldelight", false),
                    SqlDelightSourceFolder("src/sqldelight/sqldelight", false)
                ))
        )
      }
    }
  }
}