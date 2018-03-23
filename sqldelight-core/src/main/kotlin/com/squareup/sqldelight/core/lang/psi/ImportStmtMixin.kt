package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.psi.SqliteAnnotatedElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.squareup.sqldelight.core.psi.SqlDelightImportStmt
import com.squareup.sqldelight.core.psi.SqlDelightSqlStmtList

abstract class ImportStmtMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node),
    SqlDelightImportStmt,
    SqliteAnnotatedElement {
  private fun type(): String {
    return javaType.text.substringAfterLast(".")
  }

  override fun annotate(annotationHolder: SqliteAnnotationHolder) {
    if ((parent as SqlDelightSqlStmtList).importStmtList
        .filterIsInstance<ImportStmtMixin>()
        .filter { it != this }
        .any { it.type() == type() }) {
      annotationHolder.createErrorAnnotation(this, "Multiple imports for type ${type()}")
    }
  }
}