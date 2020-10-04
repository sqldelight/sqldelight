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
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

open class VerifyMigrationTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  /** Directory where the database files are copied for the migration scripts to run against. */
  @Internal lateinit var workingDirectory: File

  @Internal lateinit var sourceFolders: Iterable<File>
  @Input lateinit var properties: SqlDelightDatabaseProperties

  private val environment by lazy {
    SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = emptyList(),
        moduleName = project.name,
        properties = properties
    )
  }

  @TaskAction
  fun verifyMigrations() {
    workingDirectory.deleteRecursively()

    val catalog = createCurrentDb()
    DatabaseFilesCollector.forDatabaseFiles(sourceFolders) {
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
    val copy = dbFile.copyTo(File(workingDirectory, dbFile.name))
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

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }
}
