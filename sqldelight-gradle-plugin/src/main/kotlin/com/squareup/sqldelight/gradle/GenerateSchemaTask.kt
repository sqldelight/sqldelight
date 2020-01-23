package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
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
import java.io.File
import java.sql.DriverManager
import javax.inject.Inject

@CacheableTask
abstract class GenerateSchemaTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract val workerExecutor: WorkerExecutor

  @get:OutputDirectory
  var outputDirectory: File? = null

  @Internal lateinit var sourceFolders: Iterable<File>

  @TaskAction
  fun generateSchemaFile() {
    workerExecutor.noIsolation().submit(GenerateSchema::class.java) {
      it.sourceFolders.set(sourceFolders.filter(File::exists))
      it.outputDirectory.set(outputDirectory)
      it.moduleName.set(project.name)
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
  }

  abstract class GenerateSchema : WorkAction<GenerateSchemaWorkParameters> {
    override fun execute() {
      val environment = SqlDelightEnvironment(
          sourceFolders = parameters.sourceFolders.get(),
          dependencyFolders = emptyList(),
          moduleName = parameters.moduleName.get()
      )

      var maxVersion = 1
      environment.forMigrationFiles { migrationFile ->
        maxVersion = maxOf(maxVersion, migrationFile.version + 1)
      }

      val outputDirectory = parameters.outputDirectory.get().asFile
      DriverManager.getConnection("jdbc:sqlite:$outputDirectory/$maxVersion.db").use { connection ->
        val sourceFiles = ArrayList<SqlDelightFile>()
        environment.forSourceFiles { file -> sourceFiles.add(file as SqlDelightFile) }
        sourceFiles.forInitializationStatements { sqlText ->
          connection.prepareStatement(sqlText).execute()
        }
      }
    }
  }
}
