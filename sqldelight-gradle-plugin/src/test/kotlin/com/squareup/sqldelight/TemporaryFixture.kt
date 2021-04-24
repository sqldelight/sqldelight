package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.MINIMUM_SUPPORTED_VERSION
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.gradle.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.gradle.SqlDelightDatabaseNameImpl
import com.squareup.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import com.squareup.sqldelight.gradle.SqlDelightPropertiesFileImpl
import com.squareup.sqldelight.gradle.SqlDelightSourceFolderImpl
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnector
import java.io.File

internal class TemporaryFixture : AutoCloseable {
  val fixtureRoot = File("src/test/temporary-fixture").absoluteFile

  init {
    fixtureRoot.mkdir()
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")
    val settings = File(fixtureRoot, "settings.gradle")
    if (!settings.exists()) settings.createNewFile()
  }

  internal fun gradleFile(text: String) {
    File(fixtureRoot, "build.gradle").apply { createNewFile() }.writeText(text)
  }

  internal fun configure(runTask: String = "clean") {
    val result = GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments(runTask, "--stacktrace")
      .forwardOutput()
      .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  internal fun properties(): SqlDelightPropertiesFile {
    configure()
    return properties(fixtureRoot)!!
  }

  override fun close() {
    fixtureRoot.deleteRecursively()
  }
}

internal fun properties(fixtureRoot: File): SqlDelightPropertiesFileImpl? {
  val propertiesFile = GradleConnector.newConnector()
    .forProjectDirectory(fixtureRoot)
    .connect()
    .getModel(SqlDelightPropertiesFile::class.java)

  return SqlDelightPropertiesFileImpl(
    currentVersion = VERSION,
    minimumSupportedVersion = MINIMUM_SUPPORTED_VERSION,
    databases = propertiesFile.databases.map {
      SqlDelightDatabasePropertiesImpl(
        packageName = it.packageName,
        compilationUnits = it.compilationUnits.map {
          SqlDelightCompilationUnitImpl(
            name = it.name,
            sourceFolders = it.sourceFolders.map {
              SqlDelightSourceFolderImpl(
                folder = it.folder,
                dependency = it.dependency
              )
            },
            outputDirectoryFile = it.outputDirectoryFile,
          )
        },
        className = it.className,
        dependencies = it.dependencies.map {
          SqlDelightDatabaseNameImpl(
            packageName = it.packageName,
            className = it.className
          )
        },
        dialectPresetName = it.dialectPresetName,
        deriveSchemaFromMigrations = it.deriveSchemaFromMigrations,
        rootDirectory = it.rootDirectory
      )
    }
  )
}

internal fun withTemporaryFixture(body: TemporaryFixture.() -> Unit) {
  TemporaryFixture().use(body)
}
