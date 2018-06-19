package com.squareup.sqldelight.intellij.migrations

import com.intellij.psi.PsiDirectory
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqlite.migrations.DatabaseFilesCollector
import org.jetbrains.kotlin.idea.util.module
import java.io.File

class MigrationsContext(val sourceFolders: Iterable<PsiDirectory>, val latestDbFile: File?) {
  companion object {
    fun fromCurrentFile(file: SqlDelightFile): MigrationsContext {
      val sourceFolders = findSourceFolders(file)
      val latestDbFile = findLatestDbFile(sourceFolders)
      return MigrationsContext(sourceFolders, latestDbFile)
    }

    private fun findSourceFolders(currentFile: SqlDelightFile): Iterable<PsiDirectory> {
      val module = currentFile.module ?: return emptyList()
      val fileIndex = SqlDelightFileIndex.getInstance(module)
      if (!fileIndex.isConfigured) return emptyList()
      return fileIndex.sourceFolders(currentFile)
    }

    private fun findLatestDbFile(sourceFolders: Iterable<PsiDirectory>): File? {
      val folders = sourceFolders
          .map { File(it.virtualFile.path) }
          .toMutableList()
      var latestDbFile: File? = null
      var latestVersion = 0
      DatabaseFilesCollector.forDatabaseFiles(folders) { dbFile ->
        val version = dbFile.nameWithoutExtension.toInt()
        if (version > latestVersion) {
          latestDbFile = dbFile
          latestVersion = version
        }
      }
      return latestDbFile
    }
  }
}
