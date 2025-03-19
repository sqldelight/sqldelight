package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.TableFunctionRowType
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionAliasName
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionColumnAlias
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder

/**
 * Return the columns data types (e.g. TEXT[]) as table row types (e.g. TEXT) by zipping sqldefcolumns and row alias columns together
 * and finding the current node. e.g. zip these nodes - UNNEST(a, b) AS x(y, z).
 * Create a delegate of PostgreSqlTypeName to remove the `[]` from the columnType node so the resolver will create the non-array table row type
 */
internal abstract class TableFunctionColumnAliasMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node),
  TableFunctionRowType {
  override fun columnType(): SqlTypeName {
    val column = parent.parent.children.filterIsInstance<SqlColumnExpr>()
      .map { (it.columnName.reference!!.resolve()!!.parent as SqlColumnDef).columnType.typeName }
      .zip(
        parent.parent.children.filterIsInstance<PostgreSqlTableFunctionAliasName>()
          .flatMap { it.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>() },
      )
      .first { it.second.node == node }
    return TableRowSqlTypeName(column.first as PostgreSqlTypeName)
  }

  override val parseRule: (PsiBuilder, Int) -> Boolean = PostgreSqlParser::table_function_column_alias_real
}

/**
 * Delegate that returns single node column type without "[]" for resolving to non-array Intermediate type
 */
private class TableRowSqlTypeName(private val columnSqlTypeName: PostgreSqlTypeName) : PostgreSqlTypeName by columnSqlTypeName {
  override fun getNode(): ASTNode {
    return columnSqlTypeName.node.firstChildNode // take data type and ignore last nodes "[" "]"
  }
}
