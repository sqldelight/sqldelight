package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.impl.SqlStringLiteralImpl
import com.intellij.lang.ASTNode

internal abstract class StringLiteralMixin(node: ASTNode) : SqlStringLiteralImpl(node) {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (node.text.startsWith('"')) {
      annotationHolder.createErrorAnnotation(this, "String literals should use ' instead of \"")
    }
  }
}
