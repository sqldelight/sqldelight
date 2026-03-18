package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAtTimeZoneOperatorExpression
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * The AT TIME ZONE operator converts time stamp without time zone to/from time stamp with time zone,
 * and time with time zone values to different time zones (Note: time is not currently supported)
 *    timestamp without time zone AT TIME ZONE zone → timestamp with time zone
 *    timestamp with time zone AT TIME ZONE zone → timestamp without time zone
 */
internal abstract class AtTimeZoneOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlAtTimeZoneOperatorExpression {

  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
