package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqlite.migrations.CatalogDatabase
import com.squareup.sqlite.migrations.DatabaseFilesCollector
import com.squareup.sqlite.migrations.ObjectDifferDatabaseComparator
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class VerifyMigrationTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  lateinit var sourceFolders: Iterable<File>

  private val environment by lazy {
    SqlDelightEnvironment(sourceFolders = sourceFolders.filter { it.exists() })
  }

  @TaskAction
  fun verifyMigrations() {
    val catalog = createCurrentDb()
    DatabaseFilesCollector.forDatabaseFiles(sourceFolders) {
      checkMigration(it, catalog)
    }
  }

  private fun createCurrentDb(): CatalogDatabase {
    val sourceFiles = ArrayList<SqlDelightFile>()
    environment.forSourceFiles { file -> sourceFiles.add(file as SqlDelightFile) }
    val initStatements = ArrayList<String>()
    sourceFiles.forInitializationStatements { sqlText ->
      initStatements.add(sqlText)
    }
    return CatalogDatabase.withInitStatements(initStatements)
  }

  private fun checkMigration(dbFile: File, currentDb: CatalogDatabase) {
    val actualCatalog = createActualDb(dbFile)
    val diffReport = ObjectDifferDatabaseComparator.compare(currentDb, actualCatalog).let { diff ->
      buildString(diff::printTo)
    }

    if (diffReport.isNotEmpty()) {
      throw IllegalStateException("Error migrating from ${dbFile.name}, fresh database looks" +
          " different from migration database:\n$diffReport")
    }
  }

  private fun createActualDb(dbFile: File): CatalogDatabase {
    val version = dbFile.nameWithoutExtension.toInt()
    val copy = dbFile.copyTo(File("${project.buildDir}/sqldelight/${dbFile.name}"))
    val initStatements = ArrayList<String>()
    environment.forMigrationFiles {
      if (version > it.version) return@forMigrationFiles
      it.sqlStmtList!!.statementList.forEach {
        initStatements.add(it.rawSqlText())
      }
    }
    return CatalogDatabase.fromFile(copy.absolutePath, initStatements).also { copy.delete() }
  }
}
