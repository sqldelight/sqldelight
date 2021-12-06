package com.squareup.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File

@Suppress("UnstableApiUsage") // Worker API.
@CacheableTask
abstract class GenerateMigrationOutputTask : SqlDelightWorkerTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:OutputDirectory
  var outputDirectory: File? = null

  @Input val projectName: Property<String> = project.objects.property(String::class.java)

  @Nested lateinit var properties: SqlDelightDatabasePropertiesImpl
  @Nested lateinit var compilationUnit: SqlDelightCompilationUnitImpl
  @Input lateinit var migrationOutputExtension: String

  @TaskAction
  fun generateSchemaFile() {
    workQueue().submit(GenerateMigration::class.java) {
      it.outputDirectory.set(outputDirectory)
      it.moduleName.set(projectName)
      it.properties.set(properties)
      it.migrationExtension.set(migrationOutputExtension)
      it.compilationUnit.set(compilationUnit)
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface GenerateSchemaWorkParameters : WorkParameters {
    val outputDirectory: DirectoryProperty
    val moduleName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
    val compilationUnit: Property<SqlDelightCompilationUnit>
    val migrationExtension: Property<String>
  }

  abstract class GenerateMigration : WorkAction<GenerateSchemaWorkParameters> {

    private val sourceFolders: List<File>
      get() = parameters.compilationUnit.get().sourceFolders.map { it.folder }

    override fun execute() {
      val properties = parameters.properties.get()
      val environment = SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = emptyList(),
        moduleName = parameters.moduleName.get(),
        properties = properties,
        verifyMigrations = false,
        compilationUnit = parameters.compilationUnit.get(),
      )

      val outputDirectory = parameters.outputDirectory.get().asFile
      val migrationExtension = parameters.migrationExtension.get()

      // Clear out the output directory.
      outputDirectory.listFiles()?.forEach { it.delete() }

      // Generate the new files.
      environment.forMigrationFiles { migrationFile ->
        val output = File(
          outputDirectory,
          "${migrationFile.virtualFile!!.nameWithoutExtension}$migrationExtension"
        )
        output.writeText(
          migrationFile.sqlStmtList?.stmtList.orEmpty()
            .filterNotNull().joinToString(separator = "\n\n") { "${it.rawSqlText()};" }
        )
      }
    }
  }
}
