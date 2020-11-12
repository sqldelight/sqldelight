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

@Suppress("UnstableApiUsage") // Worker API.
@CacheableTask
abstract class GenerateMigrationOutputTask : SourceTask(), SqlDelightWorkerTask {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract override val workerExecutor: WorkerExecutor

  @get:OutputDirectory
  var outputDirectory: File? = null

  @Internal lateinit var sourceFolders: Iterable<File>
  @Input lateinit var properties: SqlDelightDatabaseProperties
  @Input lateinit var migrationOutputExtension: String

  @Input override var useClassLoaderIsolation = true

  @TaskAction
  fun generateSchemaFile() {
    workQueue().submit(GenerateMigration::class.java) {
      it.sourceFolders.set(sourceFolders.filter(File::exists))
      it.outputDirectory.set(outputDirectory)
      it.moduleName.set(project.name)
      it.properties.set(properties)
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
    val properties: Property<SqlDelightDatabaseProperties>
    val migrationExtension: Property<String>
  }

  abstract class GenerateMigration : WorkAction<GenerateSchemaWorkParameters> {
    override fun execute() {
      val properties = parameters.properties.get()
      val environment = SqlDelightEnvironment(
          sourceFolders = parameters.sourceFolders.get(),
          dependencyFolders = emptyList(),
          moduleName = parameters.moduleName.get(),
          properties = properties,
          verifyMigrations = false
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
