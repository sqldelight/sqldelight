package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.TableFunctionExprRowType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.generateSeriesRowType
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionAliasName
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionColumnAlias
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableOrSubquery
import app.cash.sqldelight.dialects.postgresql.unnestRowType
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.util.PsiTreeUtil

/**
 * Resolves the row type from a single table-function output column alias, e.g. the `y` in
 * `UNNEST(a, b) AS x(y, z)` or the `a` in `generate_series(0, 14, 7) AS s(a)`.
 *
 * The two table functions derive the type differently:
 *  - `unnest`: zip the source column definitions with the alias columns and unwrap the array (`TEXT[]` -> `TEXT`).
 *  - `generate_series`: a single column whose type is resolved from the argument expressions (same resolution
 *    as the `generate_series(...)` form).
 */
internal abstract class TableFunctionColumnAliasMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node),
  TableFunctionExprRowType {

  override fun rowType(typeResolver: TypeResolver): IntermediateType {
    val tableOrSubquery = PsiTreeUtil.getParentOfType(this, PostgreSqlTableOrSubquery::class.java)!!
    tableOrSubquery.generateSeriesTableFunction?.let {
      val args = tableOrSubquery.children.filterIsInstance<SqlExpr>()
      return typeResolver.generateSeriesRowType(args)
    }
    tableOrSubquery.jsonTableFunction?.let { jsonFunction ->
      val columnIndex = parent.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>().indexOfFirst { it.node == node }
      return jsonTableFunctionRowType(jsonFunction.name.lowercase(), columnIndex)
    }
    return typeResolver.unnestRowType(unnestSourceColumnType())
  }

  /**
   * Return the array `SqlTypeName` of the UNNEST argument that lines up with this alias node, by zipping the
   * argument expressions and the row alias columns together, e.g. zip these nodes - `UNNEST(a, b) AS x(y, z)`.
   *
   * The argument may be a column reference (`UNNEST(zipcodes)`) whose declared type is an array, or an inline
   * array-typed expression (`UNNEST(?::TEXT[])`, `UNNEST('{1,2}'::INTEGER[])`) whose type comes from the cast.
   */
  private fun unnestSourceColumnType(): SqlTypeName {
    val tableOrSubquery = parent.parent
    val argExprs = tableOrSubquery.children.filterIsInstance<SqlExpr>()
    val aliasColumns = tableOrSubquery.children.filterIsInstance<PostgreSqlTableFunctionAliasName>()
      .flatMap { it.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>() }

    return argExprs.zip(aliasColumns)
      .first { it.second.node == node }
      .first
      .unnestArrayTypeName()
  }

  override val parseRule: (PsiBuilder, Int) -> Boolean = PostgreSqlParser::table_function_column_alias_real
}

/** The array `SqlTypeName` an UNNEST argument expression contributes. */
internal fun SqlExpr.unnestArrayTypeName(): SqlTypeName = when (this) {
  is SqlColumnExpr ->
    (columnName.reference!!.resolve()!!.parent as SqlColumnDef).columnType.typeName
  else ->
    // cast expressions (?::TEXT[], '{1,2}'::INTEGER[], fn()::INTEGER[]) expose the array type after the `::`
    PsiTreeUtil.findChildrenOfType(this, SqlTypeName::class.java).last()
}
