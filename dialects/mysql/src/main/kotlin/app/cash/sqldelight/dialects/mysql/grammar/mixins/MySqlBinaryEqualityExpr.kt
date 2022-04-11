package app.cash.sqldelight.dialects.mysql.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.impl.SqlBinaryEqualityExprImpl
import com.intellij.lang.ASTNode

internal class MySqlBinaryEqualityExpr(node: ASTNode) : SqlBinaryEqualityExprImpl(node) {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (node.getChildren(null).getOrNull(2)?.text == "==") {
      annotationHolder.createErrorAnnotation(this, "Expected '=' but got '=='.")
    }
    super.annotate(annotationHolder)
  }
}
