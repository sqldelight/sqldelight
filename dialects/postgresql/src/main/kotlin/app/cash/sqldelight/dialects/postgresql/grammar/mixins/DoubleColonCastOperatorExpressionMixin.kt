package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDoubleColonCastOperatorExpression
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * Support historical double colon casts
 * <expr>::<datatype>
 * The expr is used to determine nullable when resolver casts to new type
 */
internal abstract class DoubleColonCastOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlExpr,
  PostgreSqlDoubleColonCastOperatorExpression {
  val expr get() = children.filterIsInstance<SqlExpr>().first()
}
