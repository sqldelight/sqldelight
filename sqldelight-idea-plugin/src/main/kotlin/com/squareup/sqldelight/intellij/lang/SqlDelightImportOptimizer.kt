package com.squareup.sqldelight.intellij.lang

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.psi.ImportStmtMixin
import com.squareup.sqldelight.core.lang.util.findChildOfType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.psi.SqlDelightImportStmtList
import com.squareup.sqldelight.core.psi.SqlDelightJavaTypeName

class SqlDelightImportOptimizer : ImportOptimizer {

  override fun supports(file: PsiFile): Boolean = file is SqlDelightFile || file is MigrationFile

  override fun processFile(file: PsiFile): Runnable = Runnable {
    val manager = PsiDocumentManager.getInstance(file.project)
    val document = manager.getDocument(file) ?: return@Runnable
    manager.commitDocument(document)

    val columnTypes = file.findChildrenOfType<SqlDelightJavaTypeName>()
      .map { javaTypeName -> javaTypeName.text }

    val remainingImports = file.findChildrenOfType<ImportStmtMixin>()
      .asSequence()
      .map { import -> import.text }
      .filter { import -> import.findAnyOf(columnTypes) != null }
      .sorted()
      .joinToString("\n")
    val factory = PsiFileFactory.getInstance(file.project)
    val dummyFile = factory.createFileFromText(
      "_Dummy_.${SqlDelightFileType.EXTENSION}",
      SqlDelightFileType, remainingImports
    )
    val newImportList = dummyFile.findChildOfType<SqlDelightImportStmtList>()
    if (newImportList != null) {
      file.findChildOfType<SqlDelightImportStmtList>()?.replace(newImportList)
    }
  }
}
