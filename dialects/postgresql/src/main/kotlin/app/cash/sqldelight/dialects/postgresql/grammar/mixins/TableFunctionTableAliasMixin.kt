package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.psi.impl.SqlTableAliasImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class TableFunctionTableAliasMixin(
  node: ASTNode,
) : SqlTableAliasImpl(node) {
  override fun source(): PsiElement {
    // return (parent.parent.parent as SqlJoinClauseMixin).queryExposed().first().table!!
    return (parent.parent.parent as SqlJoinClauseMixin).tablesAvailable(this).map { it.tableName }.first()
  }
}

//
//
// //return (parent.parent.parent as SqlJoinClauseMixin).queryExposed().first().table!!
// val tableOrSubquery = PsiTreeUtil.getParentOfType(this, PostgreSqlTableOrSubquery::class.java)
// if (tableOrSubquery != null && tableOrSubquery.unnestTableFunction != null) {
//  // Return the UNNEST function itself as the source
//  return tableOrSubquery.unnestTableFunction!!
// }
//
// // Fallback to default implementation if not in an UNNEST context
// return this
