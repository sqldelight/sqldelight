package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.psi.impl.SqlTableAliasImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class TableFunctionTableAliasMixin(
  node: ASTNode,
) : SqlTableAliasImpl(node) {
  override fun source(): PsiElement {
    return (parent.parent.parent as SqlJoinClauseMixin).tablesAvailable(this).map { it.tableName }.first() // TODO fix
  }
}
