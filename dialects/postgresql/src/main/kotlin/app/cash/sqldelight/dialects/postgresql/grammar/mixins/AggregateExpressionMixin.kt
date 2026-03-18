package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlArrayAggStmt
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

internal abstract class AggregateExpressionMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  PostgreSqlArrayAggStmt {
  val expr get() = children.filterIsInstance<SqlExpr>().first()
}
