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
 * Resolves the row type of a single table-function output column alias, e.g. the `y` in
 * `UNNEST(a, b) AS x(y, z)` or the `a` in `generate_series(0, 14, 7) AS s(a)`.
 *
 * The two table functions derive the type differently:
 *  - `unnest`: zip the source column definitions with the alias columns and unwrap the array (`TEXT[]` -> `TEXT`).
 *  - `generate_series`: a single column whose type is computed from the argument expressions (same resolution
 *    as the scalar `generate_series(...)` form).
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
   * Return the `SqlTypeName` of the source array column matching this alias node by zipping the source column
   * definitions and the row alias columns together, e.g. zip these nodes - `UNNEST(a, b) AS x(y, z)`.
   */
  private fun unnestSourceColumnType(): SqlTypeName = parent.parent.children.filterIsInstance<SqlColumnExpr>()
    .map { (it.columnName.reference!!.resolve()!!.parent as SqlColumnDef).columnType.typeName }
    .zip(
      parent.parent.children.filterIsInstance<PostgreSqlTableFunctionAliasName>()
        .flatMap { it.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>() },
    )
    .first { it.second.node == node }
    .first

  override val parseRule: (PsiBuilder, Int) -> Boolean = PostgreSqlParser::table_function_column_alias_real
}
