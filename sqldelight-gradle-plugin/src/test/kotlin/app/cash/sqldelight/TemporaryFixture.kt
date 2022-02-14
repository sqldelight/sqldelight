package app.cash.sqldelight

import app.cash.sqldelight.core.MINIMUM_SUPPORTED_VERSION
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabaseNameImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightPropertiesFileImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnector
import java.io.File

internal class TemporaryFixture : AutoCloseable {
  val fixtureRoot = File("src/test/temporary-fixture-${System.identityHashCode(this)}").absoluteFile

  init {
    fixtureRoot.mkdir()
    val settings = File(fixtureRoot, "settings.gradle")
    if (!settings.exists()) settings.createNewFile()
  }

  internal fun gradleFile(text: String) {
    File(fixtureRoot, "build.gradle").apply { createNewFile() }.writeText(text)
  }

  internal fun configure(runTask: String = "clean") {
    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments(runTask, "--stacktrace")
      .forwardOutput()
      .build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  internal fun properties(): SqlDelightPropertiesFile {
    configure()
    return properties(fixtureRoot)
  }

  override fun close() {
    fixtureRoot.deleteRecursively()
  }
}

internal fun properties(fixtureRoot: File): SqlDelightPropertiesFileImpl {
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
