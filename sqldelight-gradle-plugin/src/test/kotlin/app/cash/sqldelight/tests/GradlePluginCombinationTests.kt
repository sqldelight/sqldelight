package app.cash.sqldelight.tests

import app.cash.sqldelight.withTemporaryFixture
import org.junit.Test

class GradlePluginCombinationTests {
  @Test
  fun `sqldelight does not link sqlite when linkSqlite is false`() {
    withTemporaryFixture {
      gradleFile(gradleBuildScript(false))
      configure("checkForSqlite")
    }
  }

  @Test
  fun `sqldelight does link sqlite when linkSqlite is true`() {
    withTemporaryFixture {
      gradleFile(gradleBuildScript(true))
      configure("checkForSqlite")
    }
  }

  private fun gradleBuildScript(shouldLinkSqlite: Boolean): String {
    val condition = if (shouldLinkSqlite) "!" else ""
    val errorMessage = if (shouldLinkSqlite) {
      "sqlite should be linked; linkSqlite is true"
    } else {
      "sqlite should not be linked; linkSqlite is false"
    }

    return """
      |import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
      |
      |plugins {
      |  alias(libs.plugins.kotlin.multiplatform)
      |  alias(libs.plugins.sqldelight)
      |}
      |
      |sqldelight {
      |  linkSqlite = $shouldLinkSqlite
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
      |task checkForSqlite {
      |  doLast {
      |    kotlin.targets.withType(KotlinNativeTarget).each { target ->
      |      target.binaries.each { binary ->
      |        if (${condition}binary.linkerOpts.contains("-lsqlite3")) {
      |          throw new GradleException("$errorMessage")
      |        }
      |      }
      |    }
      |  }
      |}
      |
    """.trimMargin()
  }
}
