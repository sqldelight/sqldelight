package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import app.cash.sqldelight.core.psi.SqlDelightImportStmtList
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class ImportStmtMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node),
  SqlDelightImportStmt,
  SqlAnnotatedElement {
  private fun type(): String {
    return javaType.text.substringAfterLast(".")
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if ((parent as SqlDelightImportStmtList).importStmtList
      .filterIsInstance<ImportStmtMixin>()
      .filter { it != this }
      .any { it.type() == type() }
    ) {
      annotationHolder.createErrorAnnotation(this, "Multiple imports for type ${type()}")
    }
  }
}
