package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.AggregateFunctionExpression
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * The aggregates `array_agg`, `string_agg`, `json_agg` and `json_object_agg` are parsed as their own
 * expressions instead of a SqlFunctionExpr, this exposes them as an AggregateFunctionExpression for
 * the compiler.
 */
internal abstract class AggregateExpressionMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  AggregateFunctionExpression {
  val expr: SqlExpr get() = functionArguments.first()
}
