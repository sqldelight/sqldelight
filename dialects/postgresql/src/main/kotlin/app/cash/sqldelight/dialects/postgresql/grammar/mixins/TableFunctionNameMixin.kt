package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.TableFunctionExprRowType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableOrSubquery
import app.cash.sqldelight.dialects.postgresql.unnestRowType
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder

/**
 * The `unnest` keyword node when used as a set-returning function in a FROM clause, e.g.
 * `SELECT u.a FROM UNNEST('{1,2}'::INTEGER[]) AS u(a)`.
 *
 * Implements [TableFunctionExprRowType] (like generate_series) so that a standalone UNNEST
 * observes no underlying table, and so the un-aliased single-column form (`UNNEST(arr) AS r`) can
 * resolve its element type from the argument.
 */
internal abstract class TableFunctionNameMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node),
  TableFunctionExprRowType {

  override fun rowType(typeResolver: TypeResolver): IntermediateType {
    val arg = (parent as PostgreSqlTableOrSubquery).children.filterIsInstance<SqlExpr>().first()
    return typeResolver.unnestRowType(arg.unnestArrayTypeName())
  }

  override val parseRule: (builder: PsiBuilder, level: Int) -> Boolean = PostgreSqlParser::unnest_table_function_real
}
