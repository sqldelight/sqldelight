package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.squareup.sqldelight.core.SqlDelightFileIndex

class MigrationFile(
  viewProvider: FileViewProvider
) : SqlDelightFile(viewProvider, MigrationLanguage) {
  val version: Int by lazy {
    name.substringBeforeLast(".${fileType.EXTENSION}")
        .filter { it in '0'..'9' }.toIntOrNull() ?: 0
  }

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override val packageName
    get() = module?.let { module -> SqlDelightFileIndex.getInstance(module).packageName }

  override val order
    get() = version

  override fun getFileType() = MigrationFileType

  override fun iterateSqlFiles(iterator: (SqlFileBase) -> Boolean) {
    val psiManager = PsiManager.getInstance(project)
    val module = module ?: return
    val virtualFile = virtualFile ?: return
    val index = SqlDelightFileIndex.getInstance(module)
    val sourceFolders = index.sourceFolders(virtualFile)
    if (sourceFolders.isEmpty()) {
      iterator(this)
      return
    }
    sourceFolders.forEach {
      ProjectRootManager.getInstance(project).fileIndex.iterateContentUnderDirectory(it) { file ->
        val vFile = when (file.fileType) {
          MigrationFileType -> file
          DatabaseFileType -> {
            (psiManager.findViewProvider(file) as? DatabaseFileViewProvider)?.getSchemaFile()
          }
          else -> null
        } ?: return@iterateContentUnderDirectory true

        psiManager.findFile(vFile)?.let { psiFile ->
          if (psiFile is SqlFileBase) return@iterateContentUnderDirectory iterator(psiFile)
        }
        true
      }
    }
  }
}
