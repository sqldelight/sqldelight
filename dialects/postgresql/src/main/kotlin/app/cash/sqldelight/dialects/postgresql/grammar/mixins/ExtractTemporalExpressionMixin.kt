package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlExtractTemporalExpression
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 *  e.g access expr node for nullable type see `PostgreSqlTypeResolver extractTemporalExpression`
 *  EXTRACT(HOUR FROM TIME '10:30:45'),
 *  EXTRACT(DAY FROM created_date)
 */
internal abstract class ExtractTemporalExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlExpr,
  PostgreSqlExtractTemporalExpression {
  val expr get() = children.filterIsInstance<SqlExpr>().first()
}
