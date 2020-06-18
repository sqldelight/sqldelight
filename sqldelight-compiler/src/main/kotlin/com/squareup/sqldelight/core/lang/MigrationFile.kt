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
        .filter { it in '0'..'9' }.toInt()
  }

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override val packageName
    get() = SqlDelightFileIndex.getInstance(module).packageName

  override val order
    get() = version

  override fun getFileType() = MigrationFileType

  override fun iterateSqlFiles(iterator: (SqlFileBase) -> Boolean) {
    val psiManager = PsiManager.getInstance(project)
    ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
      val vFile = when (file.fileType) {
        MigrationFileType -> file
        DatabaseFileType -> {
          (psiManager.findViewProvider(file) as? DatabaseFileViewProvider)?.getSchemaFile()
        }
        else -> null
      } ?: return@iterateContent true
      psiManager.findFile(vFile)?.let { psiFile ->
        if (psiFile is SqlFileBase) return@iterateContent iterator(psiFile)
      }
      true
    }
  }
}
