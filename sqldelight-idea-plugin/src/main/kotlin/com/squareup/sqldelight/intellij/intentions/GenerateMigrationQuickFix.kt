package com.squareup.sqldelight.intellij.intentions

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqlite.migrations.CatalogDatabase
import com.squareup.sqlite.migrations.ObjectDifferDatabaseComparator
import org.jetbrains.kotlin.idea.util.module
import java.io.File
import java.io.FileWriter

class GenerateMigrationQuickFix : BaseIntentionAction() {

  private lateinit var sourceFolders: Iterable<PsiDirectory>

  override fun getFamilyName() = INTENTIONS_FAMILY_NAME_MIGRATIONS

  override fun getText() = INTENTIONS_GENERATE_MIGRATION

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
    ApplicationManager.getApplication().invokeLater {
      object : WriteCommandAction.Simple<Project>(project) {
        override fun run() {
          sourceFolders = findSourceFolders(psiFile as SqlDelightFile)
          generateMigrationScript(psiFile)
        }
      }.execute()
    }
  }

  private fun findSourceFolders(currentFile: SqlDelightFile): Iterable<PsiDirectory> {
    val module = currentFile.module ?: return emptyList()
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    if (!fileIndex.isConfigured) return emptyList()
    return fileIndex.sourceFolders(currentFile)
  }

  private fun generateMigrationScript(currentFile: SqlDelightFile) {
    val currentDb = createCurrentDb(currentFile)
    val actualDbFile = findLatestDbFile()
    if (actualDbFile != null) {
      val actualDb = createActualDb(actualDbFile)
      val diffReport = ObjectDifferDatabaseComparator.compare(currentDb, actualDb)
      if (diffReport.isEmpty()) {
        deleteMigrationFileIfExists(actualDbFile)
      } else {
        createMigrationFile(actualDbFile).use(diffReport::printTo)
      }
    }
  }

  private fun createCurrentDb(currentFile: SqlDelightFile): CatalogDatabase {
    val sourceFiles = ArrayList<SqlDelightFile>()
    currentFile.iterateSqliteFiles { sourceFiles.add(it as SqlDelightFile) }
    val initStatements = ArrayList<String>()
    sourceFiles.forInitializationStatements { initStatements.add(it) }
    return CatalogDatabase.withInitStatements(initStatements)
  }

  private fun findLatestDbFile(): File? {
    var dbFile: File? = null
    var latestVersion = 0
    val folders = sourceFolders
        .map { File(it.virtualFile.path) }
        .toMutableList()
    while (folders.isNotEmpty()) {
      val folder = folders.removeAt(0)
      folder.listFiles()?.forEach { file ->
        if (file.name.endsWith(".db")) {
          val version = file.nameWithoutExtension.toInt()
          if (version > latestVersion) {
            dbFile = file
            latestVersion = version
          }
        } else if (file.isDirectory) {
          folders.add(file)
        }
      }
    }
    return dbFile
  }

  private fun createActualDb(dbFile: File): CatalogDatabase {
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

  private fun deleteMigrationFileIfExists(dbFile: File) {
    val migrationFile = File(dbFile.absolutePath.replace(".db", ".sqm"))
    if (migrationFile.exists()) migrationFile.delete()
  }

  private fun createMigrationFile(dbFile: File): FileWriter {
    val migrationFile = File(dbFile.absolutePath.replace(".db", ".sqm"))
    return FileWriter(migrationFile)
  }
}
