package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlJsonExpression
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlLtreeExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode
import kotlinx.coroutines.NonCancellable.children

internal abstract class LtreeExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlLtreeExpression {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    if (columnType == null || columnType !in arrayOf("LTREE")) {
      annotationHolder.createErrorAnnotation(firstChild.firstChild, "Left side of ltree expression must be a ltree column.")
    }
    if ((ltreeBooleanOperator != null || ltreeBinaryOperator != null) && columnType != "LTREE") {
      annotationHolder.createErrorAnnotation(firstChild.firstChild, "Left side of ltree expression must be a ltree column.")
    }
    super.annotate(annotationHolder)
  }

  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
