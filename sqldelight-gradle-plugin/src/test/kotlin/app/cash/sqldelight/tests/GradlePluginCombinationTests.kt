package app.cash.sqldelight.tests

import app.cash.sqldelight.withTemporaryFixture
import org.junit.Test

class GradlePluginCombinationTests {
  @Test
  fun `sqldelight can be applied after kotlin-android-extensions`() {
    withTemporaryFixture {
      gradleFile(
        """
        |buildscript {
        |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
        |}
        |
        |apply plugin: 'org.jetbrains.kotlin.multiplatform'
        |apply plugin: 'com.android.application'
        |apply plugin: 'app.cash.sqldelight'
        |apply plugin: 'kotlin-android-extensions'
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
        |androidExtensions {
        |    experimental = true
        |}
        |
        |android {
        |  compileSdkVersion deps.versions.compileSdk.get() as int
        |}
        |
        |kotlin {
        |  android()
        |}
      """.trimMargin()
      )

      configure()
    }
  }

  @Test
  fun `sqldelight fails when linkSqlite=false on native without additional linker settings`() {
    withTemporaryFixture {
      gradleFile(
        """
    |buildscript {
    |  apply from: "${"$"}{projectDir.absolutePath}/../buildscript.gradle"
    |}
    |
    |apply plugin: 'org.jetbrains.kotlin.multiplatform'
    |apply plugin: 'app.cash.sqldelight'
    |
    |repositories {
    |  maven {
    |    url "file://${"$"}{rootDir}/../../../../build/localMaven"
    |  }
    |}
    |
    |sqldelight {
    |  linkSqlite = false
    |  CommonDb {
    |    packageName = "com.sample"
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
    """.trimMargin()
      )
      configure("checkForSqlite")
    }
  }
}
