package app.cash.sqldelight.tests

import app.cash.sqldelight.withTemporaryFixture
import org.junit.Test

class GradlePluginCombinationTests {
  @Test
  fun `sqldelight can be applied after kotlin-android-extensions`() {
    withTemporaryFixture {
      gradleFile(
        """
        |plugins {
        |  alias(libs.plugins.kotlin.multiplatform)
        |  alias(libs.plugins.android.application)
        |  alias(libs.plugins.sqldelight)
        |  alias(libs.plugins.kotlin.android.extensions)
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
        |androidExtensions {
        |    experimental = true
        |}
        |
        |android {
        |  namespace 'com.example.namespace'
        |  compileSdk libs.versions.compileSdk.get() as int
        |}
        |
        |kotlin {
        |  android()
        |}
        """.trimMargin(),
      )

      configure()
    }
  }

  @Test
  fun `sqldelight fails when linkSqlite=false on native without additional linker settings`() {
    withTemporaryFixture {
      gradleFile(
        """
    |plugins {
    |  alias(libs.plugins.kotlin.multiplatform)
    |  alias(libs.plugins.sqldelight)
    |}
    |
    |sqldelight {
    |  linkSqlite = false
    |  databases {
    |    CommonDb {
    |      packageName = "com.sample"
    |    }
    |  }
    |}
    |
    |kotlin {
    |  iosX64 {
    |    binaries { framework() }
    |  }
    |}
    |
    |import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
    |
    |task checkForSqlite {
    |  doLast {
    |    // Verify no kotlin compile tasks have "-lsqlite3" in their extraOpts
    |    tasks.withType(AbstractKotlinNativeCompile.class) { task ->
    |      if (task.additionalCompilerOptions.get().contains("-lsqlite3")) throw new GradleException("sqlite should not be linked; linkSqlite is false")
    |    }
    |  }
    |}
    |
        """.trimMargin(),
      )
      configure("checkForSqlite")
    }
  }
}
