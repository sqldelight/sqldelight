package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
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

@Suppress("UnstableApiUsage") // Worker API
@CacheableTask
abstract class GenerateSchemaTask : SourceTask(), SqlDelightWorkerTask {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract override val workerExecutor: WorkerExecutor

  @get:OutputDirectory
  var outputDirectory: File? = null

  @Internal lateinit var sourceFolders: Iterable<File>
  @Input lateinit var properties: SqlDelightDatabaseProperties

  @Input var verifyMigrations: Boolean = false

  @Input override var useClassLoaderIsolation = true

  @TaskAction
  fun generateSchemaFile() {
    workQueue.submit(GenerateSchema::class.java) {
      it.sourceFolders.set(sourceFolders.filter(File::exists))
      it.outputDirectory.set(outputDirectory)
      it.moduleName.set(project.name)
      it.properties.set(properties)
      it.verifyMigrations.set(verifyMigrations)
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
    val verifyMigrations: Property<Boolean>
  }

  abstract class GenerateSchema : WorkAction<GenerateSchemaWorkParameters> {
    override fun execute() {
      val environment = SqlDelightEnvironment(
          sourceFolders = parameters.sourceFolders.get(),
          dependencyFolders = emptyList(),
          moduleName = parameters.moduleName.get(),
          properties = parameters.properties.get(),
          verifyMigrations = parameters.verifyMigrations.get()
      )

      var maxVersion = 1
      environment.forMigrationFiles { migrationFile ->
        maxVersion = maxOf(maxVersion, migrationFile.version + 1)
      }

      val outputDirectory = parameters.outputDirectory.get().asFile
      File("$outputDirectory/$maxVersion.db").apply {
        if (exists()) delete()
      }
      createConnection("$outputDirectory/$maxVersion.db").use { connection ->
        val sourceFiles = ArrayList<SqlDelightQueriesFile>()
        environment.forSourceFiles { file ->
          if (file is SqlDelightQueriesFile) sourceFiles.add(file)
        }
        sourceFiles.forInitializationStatements { sqlText ->
          connection.prepareStatement(sqlText).execute()
        }
      }
    }

    private fun createConnection(path: String): Connection {
      return try {
        DriverManager.getConnection("jdbc:sqlite:$path")
      } catch (e: SQLException) {
        DriverManager.getConnection("jdbc:sqlite:$path")
      }
    }
  }
}
