package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.psi.SqliteAnnotatedElement
import com.alecstrong.sqlite.psi.core.psi.SqliteIdentifier
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier

abstract class StmtIdentifierMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node),
    SqlDelightStmtIdentifier,
    SqliteAnnotatedElement {
  override fun getName() = identifier()?.text

  override fun setName(p0: String): PsiElement {
    return this
  }

  override fun annotate(annotationHolder: SqliteAnnotationHolder) {
    if (name != null && (containingFile as SqlDelightFile).sqliteStatements()
        .filterNot { it.identifier == this }
        .any { it.identifier.name == name }) {
      annotationHolder.createErrorAnnotation(this, "Duplicate SQL identifier")
    }
  }

  override fun identifier() = children.filterIsInstance<SqliteIdentifier>().singleOrNull()
}