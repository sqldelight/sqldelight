package app.cash.sqldelight

import app.cash.sqldelight.core.MINIMUM_SUPPORTED_VERSION
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabaseNameImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightPropertiesFileImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.GradleConnector

internal class TemporaryFixture : AutoCloseable {
  val fixtureRoot = File("src/test/temporary-fixture-${System.identityHashCode(this)}").absoluteFile

  init {
    fixtureRoot.mkdir()
    val settings = File(fixtureRoot, "settings.gradle")
    if (!settings.exists()) {
      settings.createNewFile()
      settings.writeText(
        """
        |pluginManagement {
        |  includeBuild("../build-logic-tests")
        |}
        |
        |plugins {
        |  id("sqldelightTests")
        |}
        |
        """.trimMargin(),
      )
    }
  }

  internal fun gradleFile(text: String) {
    File(fixtureRoot, "build.gradle").apply { createNewFile() }.writeText(text)
  }

  internal fun configure(runTask: String = "clean", enableIsolatedProject: Boolean = true) {
    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot, enableIsolatedProject)
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
    .use { connection ->
      connection.action(FetchSqlDelightModel(fixtureRoot))
        .run()
    }

  return SqlDelightPropertiesFileImpl(
    currentVersion = VERSION,
    minimumSupportedVersion = MINIMUM_SUPPORTED_VERSION,
    dialectJars = listOf(File("test")),
    databases = propertiesFile.databases.map {
      SqlDelightDatabasePropertiesImpl(
        packageName = it.packageName,
        compilationUnits = it.compilationUnits.map {
          SqlDelightCompilationUnitImpl(
            name = it.name,
            sourceFolders = it.sourceFolders.map {
              SqlDelightSourceFolderImpl(
                folder = it.folder,
                dependency = it.dependency,
              )
            }.toSet(),
            outputDirectoryFile = it.outputDirectoryFile,
          )
        },
        className = it.className,
        dependencies = it.dependencies.map {
          SqlDelightDatabaseNameImpl(
            packageName = it.packageName,
            className = it.className,
          )
        },
        deriveSchemaFromMigrations = it.deriveSchemaFromMigrations,
        treatNullAsUnknownForEquality = it.treatNullAsUnknownForEquality,
        rootDirectory = it.rootDirectory,
        codegenExcludedColumns = it.codegenExcludedColumns,
      )
    },
  )
}

private class FetchSqlDelightModel(
  private val projectDir: File,
) : BuildAction<SqlDelightPropertiesFile> {
  override fun execute(controller: BuildController): SqlDelightPropertiesFile {
    val target = controller.buildModel.projects.first {
      it.projectDirectory == projectDir
    }
    return controller.getModel(target, SqlDelightPropertiesFile::class.java)
  }
}

internal fun withTemporaryFixture(body: TemporaryFixture.() -> Unit) {
  TemporaryFixture().use(body)
}
