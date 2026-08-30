package app.cash.sqldelight.dialects.sqlite_3_44.grammar.mixins

import app.cash.sqldelight.dialect.api.AggregateFunctionExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.ASTNode

/**
 * `group_concat` and `string_agg` are parsed as an aggregate function expression instead of a
 * SqlFunctionExpr, so expose them as an AggregateFunctionExpression for the compiler.
 *
 * Sqlite only allows `DISTINCT` on an aggregate with a single argument and always requires the
 * separator argument of `string_agg`, use error annotation.
 */
internal abstract class AggregateFunctionExprMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node),
  AggregateFunctionExpression {
  private val distinct get() = node.findChildByType(SqlTypes.DISTINCT)

  private val separator get() = functionArguments.getOrNull(1)

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)
    if (functionName.lowercase() == "string_agg" && separator == null) {
      annotationHolder.createErrorAnnotation(this, "Wrong number of arguments to function string_agg()")
    } else if (distinct != null && separator != null) {
      annotationHolder.createErrorAnnotation(this, "DISTINCT aggregates must have exactly one argument")
    }
  }
}
