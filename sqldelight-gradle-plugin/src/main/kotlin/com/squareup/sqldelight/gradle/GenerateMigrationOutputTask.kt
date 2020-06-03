package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.util.rawSqlText
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class GenerateMigrationOutputTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract val workerExecutor: WorkerExecutor

  @get:OutputDirectory
  var outputDirectory: File? = null

  @Internal lateinit var sourceFolders: Iterable<File>
  @Input lateinit var properties: SqlDelightDatabaseProperties
  @Input lateinit var migrationOutputExtension: String

  @TaskAction
  fun generateSchemaFile() {
    workerExecutor.noIsolation().submit(GenerateSchema::class.java) {
      it.sourceFolders.set(sourceFolders.filter(File::exists))
      it.outputDirectory.set(outputDirectory)
      it.moduleName.set(project.name)
      it.propertiesJson.set(properties.toJson())
      it.migrationExtension.set(migrationOutputExtension)
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface GenerateSchemaWorkParameters : WorkParameters {
    val sourceFolders: ListProperty<File>
    val outputDirectory: DirectoryProperty
    val moduleName: Property<String>
    val propertiesJson: Property<String>
    val migrationExtension: Property<String>
  }

  abstract class GenerateSchema : WorkAction<GenerateSchemaWorkParameters> {
    override fun execute() {
      val properties = SqlDelightDatabaseProperties.fromText(parameters.propertiesJson.get())!!
      val environment = SqlDelightEnvironment(
          sourceFolders = parameters.sourceFolders.get(),
          dependencyFolders = emptyList(),
          moduleName = parameters.moduleName.get(),
          properties = properties
      )

      val outputDirectory = parameters.outputDirectory.get().asFile
      val migrationExtension = parameters.migrationExtension.get()

      // Clear out the output directory.
      outputDirectory.listFiles().forEach { it.delete() }

      // Generate the new files.
      environment.forMigrationFiles { migrationFile ->
        val output = File(
            outputDirectory,
            "${migrationFile.virtualFile.nameWithoutExtension}$migrationExtension"
        )
        output.writeText(
            (migrationFile.sqlStmtList?.stmtList ?: emptyList())
                .filterNotNull().joinToString(separator = "\n\n") { "${it.rawSqlText()};" }
        )
      }
    }
  }
}
