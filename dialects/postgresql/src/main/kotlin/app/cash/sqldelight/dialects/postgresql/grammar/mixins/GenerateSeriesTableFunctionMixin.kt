package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.TableFunctionExprRowType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.generateSeriesRowType
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableOrSubquery
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder

/**
 * The `generate_series` keyword node when used as a set-returning function in a FROM clause, e.g.
 * `SELECT g FROM generate_series(1, 2, 10) AS g`. The single output column's type is derived from
 * the argument expressions.
 */
internal abstract class GenerateSeriesTableFunctionMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node),
  TableFunctionExprRowType {

  override fun rowType(typeResolver: TypeResolver): IntermediateType {
    val args = (parent as PostgreSqlTableOrSubquery).children.filterIsInstance<SqlExpr>()
    return typeResolver.generateSeriesRowType(args)
  }

  override val parseRule: (builder: PsiBuilder, level: Int) -> Boolean = PostgreSqlParser::generate_series_table_function_real
}
