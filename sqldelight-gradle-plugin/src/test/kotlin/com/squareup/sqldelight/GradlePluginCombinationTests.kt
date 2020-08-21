package com.squareup.sqldelight

import org.junit.Test

class GradlePluginCombinationTests {
  @Test
  fun `sqldelight can be applied after kotlin-android-extensions`() {
    withTemporaryFixture {
      gradleFile("""
        |plugins {
        |    id 'kotlin-multiplatform'
        |    id 'com.android.application'
        |    id 'kotlin-android-extensions'
        |    id 'com.squareup.sqldelight'
        |}
        |
        |apply from: "${'$'}{rootDir}/../../../../gradle/dependencies.gradle"
        |
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
        |  compileSdkVersion versions.compileSdk
        |}
        |
        |kotlin {
        |  android()
        |}
      """.trimMargin())

      configure()
    }
  }

  @Test
  fun `sqldelight fails when linkSqlite=false on native without additional linker settings`() {
    withTemporaryFixture {
      gradleFile("""
    |plugins {
    |    id 'kotlin-multiplatform'
    |    id 'com.squareup.sqldelight'
    |}
    |
    |apply from: "${'$'}{rootDir}/../../../../gradle/dependencies.gradle"
    |
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
    |      if (task.additionalCompilerOptions.contains("-lsqlite3")) throw new GradleException("sqlite should not be linked; linkSqlite is false")
    |    }
    |  }
    |}
    |
    """.trimMargin())
      configure("checkForSqlite")
    }
  }
}
