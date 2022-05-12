package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.lang.types.typeResolver
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.impl.SqlFunctionExprImpl
import com.intellij.lang.ASTNode

internal class FunctionExprMixin(node: ASTNode?) : SqlFunctionExprImpl(node) {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (parent is SqlResultColumn && typeResolver.functionType(this) == null) {
      annotationHolder.createErrorAnnotation(this, "Unknown function ${functionName.text}")
    }
  }
}
