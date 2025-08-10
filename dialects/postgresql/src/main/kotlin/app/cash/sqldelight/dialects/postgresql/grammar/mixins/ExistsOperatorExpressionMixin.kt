package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlExistsOperatorExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

internal abstract class ExistsOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlExistsOperatorExpression {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
