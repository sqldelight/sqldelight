package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlExpr

/**
 * An aggregate function which a dialect parses as its own expression rather than as a SqlFunctionExpr,
 * for example Sqlite 3.44 `group_concat(name, ',' ORDER BY name)` or PostgreSql `string_agg(name, ',') ORDER BY name`.
 *
 * Implementing AggregateFunctionExpression treats the expression as an aggregate when working out the
 * nullability of a query's columns.
 *
 * The defaults assume the grammar of `name ( arguments ) [ FILTER ( WHERE condition ) ]`.
 */
interface AggregateFunctionExpression : SqlAnnotatedElement {
  val functionName: String get() = node.firstChildNode.text

  /** The expressions being aggregated, whose nullability the result of the aggregate follows. */
  val functionArguments: List<SqlExpr>
    get() {
      val filterClause = filterClauseOffset() ?: Int.MAX_VALUE
      return children.filterIsInstance<SqlExpr>().filter { it.node.startOffset < filterClause }
    }

  /**
   * True if the aggregate can return NULL for a group of non null functionArguments, for example
   * because a FILTER clause can exclude every row of a group.
   */
  val canReturnNullForNonNullArguments: Boolean get() = filterClauseOffset() != null
}

private fun AggregateFunctionExpression.filterClauseOffset(): Int? = node.getChildren(null)
  .firstOrNull { it.text.equals("FILTER", ignoreCase = true) }
  ?.startOffset
