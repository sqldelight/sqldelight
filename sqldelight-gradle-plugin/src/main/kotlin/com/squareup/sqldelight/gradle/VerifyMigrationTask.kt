package com.squareup.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.lang.util.allowsReferenceCycles
import app.cash.sqldelight.core.lang.util.forInitializationStatements
import app.cash.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqlite.migrations.CatalogDatabase
import com.squareup.sqlite.migrations.ObjectDifferDatabaseComparator
import com.squareup.sqlite.migrations.findDatabaseFiles
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@Suppress("UnstableApiUsage") // Worker API
@CacheableTask
abstract class VerifyMigrationTask : SqlDelightWorkerTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract override val workerExecutor: WorkerExecutor

  @Input val projectName: Property<String> = project.objects.property(String::class.java)

  /** Directory where the database files are copied for the migration scripts to run against. */
  @Internal lateinit var workingDirectory: File

  @Nested lateinit var properties: SqlDelightDatabasePropertiesImpl
  @Nested lateinit var compilationUnit: SqlDelightCompilationUnitImpl

  @Input var verifyMigrations: Boolean = false

  /* Tasks without an output are never considered UP-TO-DATE by Gradle. Adding an output file that's created when the
   * task completes successfully works around the lack of an output for this task. There may be a better solution once
   * https://github.com/gradle/gradle/issues/14223 is resolved. */
  @OutputFile
  internal fun getDummyOutputFile(): File = File(temporaryDir, "success.txt")

  @TaskAction
  fun verifyMigrations() {
    runCatching {
      val workQueue = workQueue()
      workQueue.submit(VerifyMigrationAction::class.java) {
        it.workingDirectory.set(workingDirectory)
        it.projectName.set(projectName)
        it.properties.set(properties)
        it.verifyMigrations.set(verifyMigrations)
        it.compilationUnit.set(compilationUnit)
      }
      workQueue.await()
    }.onSuccess {
      getDummyOutputFile().createNewFile()
    }.onFailure {
      throw it
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface VerifyMigrationWorkParameters : WorkParameters {
    val workingDirectory: DirectoryProperty
    val projectName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
    val compilationUnit: Property<SqlDelightCompilationUnit>
    val verifyMigrations: Property<Boolean>
  }

  abstract class VerifyMigrationAction : WorkAction<VerifyMigrationWorkParameters> {
    private val logger = Logging.getLogger(VerifyMigrationTask::class.java)

    private val sourceFolders: List<File>
      get() = parameters.compilationUnit.get().sourceFolders.map { it.folder }

    private val environment by lazy {
      SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = emptyList(),
        moduleName = parameters.projectName.get(),
        properties = parameters.properties.get(),
        verifyMigrations = parameters.verifyMigrations.get(),
        compilationUnit = parameters.compilationUnit.get(),
      )
    }

    override fun execute() {
      parameters.workingDirectory.get().asFile.deleteRecursively()

      val catalog = createCurrentDb()

      sourceFolders.asSequence()
        .findDatabaseFiles()
        .forEach { dbFile ->
          checkMigration(dbFile, catalog)
        }

      checkForGaps()
    }

    private fun createCurrentDb(): CatalogDatabase {
      val sourceFiles = ArrayList<SqlDelightQueriesFile>()
      environment.forSourceFiles { file ->
        if (file is SqlDelightQueriesFile) sourceFiles.add(file)
      }
      val initStatements = ArrayList<String>()
      sourceFiles.forInitializationStatements(
        environment.dialectPreset.allowsReferenceCycles
      ) { sqlText ->
        initStatements.add(sqlText)
      }
      return CatalogDatabase.withInitStatements(initStatements)
    }

    private fun checkMigration(dbFile: File, currentDb: CatalogDatabase) {
      val actualCatalog = createActualDb(dbFile)
      val databaseComparator = ObjectDifferDatabaseComparator(
        circularReferenceExceptionLogger = {
          logger.debug(it)
        }
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
        check(it.name.any { it in '0'..'9' }) {
          "Migration files must have an integer value somewhere in their filename but ${it.name} does not."
        }
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
