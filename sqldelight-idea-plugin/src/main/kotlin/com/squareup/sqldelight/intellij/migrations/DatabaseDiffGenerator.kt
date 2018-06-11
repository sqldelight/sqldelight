package com.squareup.sqldelight.intellij.migrations

import com.intellij.psi.PsiDirectory
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqlite.migrations.CatalogDatabase
import com.squareup.sqlite.migrations.DatabaseDiff
import com.squareup.sqlite.migrations.ObjectDifferDatabaseComparator
import java.io.File

object DatabaseDiffGenerator {
  /**
   * @return A [DatabaseDiff] between the latest DB snapshot and a DB schema, created from all
   * available SQL scripts, including the latest version of the script that's currently being
   * edited. null if there are no DB snapshots to compare against.
   */
  fun generateDatabaseDiff(currentFile: SqlDelightFile, context: MigrationsContext): DatabaseDiff? {
    val currentDb = createCurrentDb(currentFile)
    if (context.latestDbFile != null) {
      val actualDb = createActualDb(context.latestDbFile, context.sourceFolders)
      return ObjectDifferDatabaseComparator.compare(currentDb, actualDb)
    }
    return null
  }

  private fun createCurrentDb(currentFile: SqlDelightFile): CatalogDatabase {
    val sourceFiles = ArrayList<SqlDelightFile>()
    currentFile.iterateSqliteFiles { sourceFiles.add(it as SqlDelightFile) }
    val initStatements = ArrayList<String>()
    sourceFiles.forInitializationStatements { initStatements.add(it) }
    return CatalogDatabase.withInitStatements(initStatements)
  }

  private fun createActualDb(dbFile: File, sourceFolders: Iterable<PsiDirectory>): CatalogDatabase {
    val version = dbFile.nameWithoutExtension.toInt()
    // TODO needs a better solution
    val copy = dbFile.copyTo(File("sqldelight-tmp-db/${dbFile.name}"))
    val initStatements = ArrayList<String>()
    // TODO not tested
    sourceFolders.flatMap { it.findChildrenOfType<MigrationFile>() }.forEach {
      if (version > it.version) return@forEach
      it.sqlStmtList!!.statementList.forEach {
        initStatements.add(it.rawSqlText())
      }
    }
    return CatalogDatabase.fromFile(copy.absolutePath, initStatements).also { copy.delete() }
  }
}
