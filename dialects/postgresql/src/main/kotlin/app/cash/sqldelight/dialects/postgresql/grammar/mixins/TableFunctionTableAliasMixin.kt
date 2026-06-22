package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableOrSubquery
import com.alecstrong.sql.psi.core.psi.impl.SqlTableAliasImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal abstract class TableFunctionTableAliasMixin(
  node: ASTNode,
) : SqlTableAliasImpl(node) {
  override fun source(): PsiElement {
    // `generate_series(...) AS g` / `json_object_keys(...) AS k`: the alias renames the function's
    // single output column, so the alias resolves to the function node whose row type the column
    // type is derived from.
    PsiTreeUtil.getParentOfType(this, PostgreSqlTableOrSubquery::class.java)?.let { tableOrSubquery ->
      tableOrSubquery.generateSeriesTableFunction?.let { return it }
      tableOrSubquery.jsonTableFunction?.let { return it }
    }
    return (parent.parent.parent as SqlJoinClauseMixin).tablesAvailable(this).map { it.tableName }.first() // TODO fix
  }
}
