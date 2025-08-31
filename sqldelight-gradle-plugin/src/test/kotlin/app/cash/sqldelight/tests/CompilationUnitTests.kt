package app.cash.sqldelight.tests

import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import app.cash.sqldelight.withTemporaryFixture
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class CompilationUnitTests {
  @Test
  fun `JVM kotlin`() {
    withTemporaryFixture {
      gradleFile(
        """
        |plugins {
        |  alias(libs.plugins.kotlin.jvm)
        |  alias(libs.plugins.sqldelight)
        |}
        |
        |sqldelight {
        |  databases {
        |    CommonDb {
        |      packageName = "com.sample"
        |    }
        |  }
        |}
        """.trimMargin(),
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "main",
            sourceFolders = setOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false)),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/main"),
          ),
        )
      }
    }
  }

  @Test
  fun `JVM kotlin with multiple databases`() {
    withTemporaryFixture {
      gradleFile(
        """
        |plugins {
        |  alias(libs.plugins.kotlin.jvm)
        |  alias(libs.plugins.sqldelight)
        |}
        |
        |sqldelight {
        |  databases {
        |    CommonDb {
        |      packageName = "com.sample"
        |    }
        |
        |    OtherDb {
        |      packageName = "com.sample.otherdb"
        |      srcDirs('src/main/sqldelight', 'src/main/otherdb')
        |      treatNullAsUnknownForEquality = true
        |    }
        |  }
        |}
        """.trimMargin(),
      )

      properties().let { properties ->
        assertThat(properties.databases).containsExactly(
          SqlDelightDatabasePropertiesImpl(
            className = "CommonDb",
            packageName = "com.sample",
            compilationUnits = listOf(
              SqlDelightCompilationUnitImpl(
                name = "main",
                sourceFolders = setOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false)),
                outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/main"),
              ),
            ),
            dependencies = emptyList(),
            rootDirectory = fixtureRoot,
          ),
          SqlDelightDatabasePropertiesImpl(
            className = "OtherDb",
            packageName = "com.sample.otherdb",
            compilationUnits = listOf(
              SqlDelightCompilationUnitImpl(
                name = "main",
                sourceFolders = setOf(
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/otherdb"), false),
                  SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
                ),
                outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/OtherDb/main"),
              ),
            ),
            dependencies = emptyList(),
            rootDirectory = fixtureRoot,
            treatNullAsUnknownForEquality = true,
          ),
        )
      }
    }
  }

  @Test
  fun `Multiplatform project with multiple targets`() {
    withTemporaryFixture {
      gradleFile(
        """
        |plugins {
        |  alias(libs.plugins.kotlin.multiplatform)
        |  alias(libs.plugins.sqldelight)
        |}
        |
        |sqldelight {
        |  databases {
        |    CommonDb {
        |      packageName = "com.sample"
        |    }
        |  }
        |}
        |
        |kotlin {
        |  iosX64()
        |  iosArm64()
        |  macosArm64()
        |  macosX64()
        |  js().nodejs()
        |  jvm()
        |}
        """.trimMargin(),
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "commonMain",
            sourceFolders = setOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/commonMain"),
          ),
        )
      }
    }
  }

  @Test
  fun `Multiplatform project with android and ios targets`() {
    withTemporaryFixture {
      gradleFile(
        """
        |plugins {
        |  alias(libs.plugins.kotlin.multiplatform)
        |  alias(libs.plugins.android.application)
        |  alias(libs.plugins.sqldelight)
        |}
        |
        |sqldelight {
        |  databases {
        |    CommonDb {
        |      packageName = "com.sample"
        |    }
        |  }
        |}
        |
        |android {
        |  namespace 'com.example.namespace'
        |  compileSdk libs.versions.compileSdk.get() as int
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
        |  androidTarget("androidLib")
        |  iosX64()
        |}
        """.trimMargin(),
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "commonMain",
            sourceFolders = setOf(SqlDelightSourceFolderImpl(File(fixtureRoot, "src/commonMain/sqldelight"), false)),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/commonMain"),
          ),
        )
      }
    }
  }

  @Test
  fun `android project with multiple flavors`() {
    withTemporaryFixture {
      gradleFile(
        """
        |plugins {
        |  alias(libs.plugins.android.application)
        |  alias(libs.plugins.kotlin.android)
        |  alias(libs.plugins.sqldelight)
        |}
        |
        |sqldelight {
        |  databases {
        |    CommonDb {
        |      packageName = "com.sample"
        |    }
        |  }
        |}
        |
        |android {
        |  namespace 'com.example.namespace'
        |  compileSdk libs.versions.compileSdk.get() as int
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
        """.trimMargin(),
      )

      properties().let { properties ->
        assertThat(properties.databases).hasSize(1)

        val database = properties.databases[0]
        assertThat(database.className).isEqualTo("CommonDb")
        assertThat(database.packageName).isEqualTo("com.sample")
        assertThat(database.compilationUnits).containsExactly(
          SqlDelightCompilationUnitImpl(
            name = "minApi23DemoDebug",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23DemoDebug/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23DemoDebug"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23DemoRelease",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23DemoRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23DemoRelease"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23DemoSqldelight",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23DemoSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23DemoSqldelight"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23FullDebug",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23FullDebug/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23FullDebug"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23FullRelease",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23FullRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23FullRelease"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi23FullSqldelight",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23FullSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23FullSqldelight"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21DemoDebug",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21DemoDebug/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21DemoDebug"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21DemoRelease",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21DemoRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21DemoRelease"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21DemoSqldelight",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Demo/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21DemoSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21DemoSqldelight"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21FullDebug",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21FullDebug/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21FullDebug"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21FullRelease",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21FullRelease/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21FullRelease"),
          ),
          SqlDelightCompilationUnitImpl(
            name = "minApi21FullSqldelight",
            sourceFolders = setOf(
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Full/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21FullSqldelight/sqldelight"), false),
              SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
            ),
            outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21FullSqldelight"),
          ),
        )
      }
    }
  }
}
