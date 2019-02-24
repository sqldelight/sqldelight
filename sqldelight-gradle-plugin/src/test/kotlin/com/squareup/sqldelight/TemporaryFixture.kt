package com.squareup.sqldelight

import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal class TemporaryFixture : AutoCloseable {
  private val fixtureRoot = File("src/test/temporary-fixture")
  private val ideaDirectory = File(fixtureRoot, ".idea")

  init {
    fixtureRoot.mkdir()
    ideaDirectory.mkdir()
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")
  }

  internal fun gradleFile(text: String) {
    File(fixtureRoot, "build.gradle").apply { createNewFile() }.writeText(text)
  }

  internal fun properties(): SqlDelightPropertiesFile {
    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "--stacktrace")
        .forwardOutput()
        .build()

    return SqlDelightPropertiesFile.fromFile(
        file = File(ideaDirectory, "sqldelight/${SqlDelightPropertiesFile.NAME}")
    )
  }

  override fun close() {
    fixtureRoot.deleteRecursively()
  }
}

internal fun withTemporaryFixture(body: TemporaryFixture.() -> Unit) {
  TemporaryFixture().use(body)
}