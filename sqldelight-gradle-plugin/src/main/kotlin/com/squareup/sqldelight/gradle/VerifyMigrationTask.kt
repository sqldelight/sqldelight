package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqlite.migrations.CatalogDatabase
import com.squareup.sqlite.migrations.DatabaseFilesCollector
import com.squareup.sqlite.migrations.ObjectDifferDatabaseComparator
import java.io.File
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
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
abstract class VerifyMigrationTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract val workerExecutor: WorkerExecutor

  /** Directory where the database files are copied for the migration scripts to run against. */
  @Internal lateinit var workingDirectory: File

  @Internal lateinit var sourceFolders: Iterable<File>
  @Input lateinit var properties: SqlDelightDatabaseProperties

  @Input var verifyMigrations: Boolean = false

  @TaskAction
  fun verifyMigrations() {
    workerExecutor.classLoaderIsolation().submit(VerifyMigrationAction::class.java) {
      it.workingDirectory.set(workingDirectory)
      it.projectName.set(project.name)
      it.sourceFolders.set(sourceFolders.filter(File::exists))
      it.properties.set(properties)
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface VerifyMigrationWorkParameters : WorkParameters {
    val sourceFolders: ListProperty<File>
    val workingDirectory: DirectoryProperty
    val projectName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
  }

  abstract class VerifyMigrationAction : WorkAction<VerifyMigrationWorkParameters> {
    private val logger = Logging.getLogger(VerifyMigrationTask::class.java)

    private val environment by lazy {
      SqlDelightEnvironment(
          sourceFolders = parameters.sourceFolders.get().filter { it.exists() },
          dependencyFolders = emptyList(),
          moduleName = parameters.projectName.get(),
          properties = parameters.properties.get()
      )
    }

    override fun execute() {
      parameters.workingDirectory.get().asFile.deleteRecursively()

      val catalog = createCurrentDb()
      DatabaseFilesCollector.forDatabaseFiles(parameters.sourceFolders.get()) {
        checkMigration(it, catalog)
      }

      checkForGaps()
    }

    private fun createCurrentDb(): CatalogDatabase {
      val sourceFiles = ArrayList<SqlDelightQueriesFile>()
      environment.forSourceFiles { file ->
        if (file is SqlDelightQueriesFile) sourceFiles.add(file)
      }
      val initStatements = ArrayList<String>()
      sourceFiles.forInitializationStatements { sqlText ->
        initStatements.add(sqlText)
      }
      return CatalogDatabase.withInitStatements(initStatements)
    }

    private fun checkMigration(dbFile: File, currentDb: CatalogDatabase) {
      val actualCatalog = createActualDb(dbFile)
      val databaseComparator = ObjectDifferDatabaseComparator(circularReferenceExceptionLogger = {
        logger.debug(it) }
      )
      val diffReport = databaseComparator.compare(currentDb, actualCatalog).let { diff ->
        buildString(diff::printTo)
      }

      check(diffReport.isEmpty()) {
        "Error migrating from ${dbFile.name}, fresh database looks" +
            " different from migration database:\n$diffReport"
      }
    }

    private fun createActualDb(dbFile: File): CatalogDatabase {
      val version = dbFile.nameWithoutExtension.toInt()
      val copy = dbFile.copyTo(File(parameters.workingDirectory.get().asFile, dbFile.name))
      val initStatements = ArrayList<String>()
      environment.forMigrationFiles {
        if (version > it.version) return@forMigrationFiles
        it.sqlStmtList!!.stmtList.forEach {
          initStatements.add(it.rawSqlText())
        }
      }
      return CatalogDatabase.fromFile(copy.absolutePath, initStatements).also { copy.delete() }
    }

    private fun checkForGaps() {
      var lastMigrationVersion: Int? = null
      environment.forMigrationFiles {
        val actual = it.version
        val expected = lastMigrationVersion?.plus(1) ?: actual
        check(actual == expected) {
          "Gap in migrations detected. Expected migration $expected, got $actual."
        }

        lastMigrationVersion = actual
      }
    }
  }
}
