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
      """.trimMargin())

      configure()
    }
  }
}