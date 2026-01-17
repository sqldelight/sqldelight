package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
/**
 * Comment On Table, View and Column.
 * Currently limited to table queries - other schema elements like Indices are not queryable outside sql-psi.
 */
internal abstract class CommentOnMixin(
  node: ASTNode,
) : SqlCompositeElementImpl(node) {

  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    return tablesAvailable(child).map { it.query }
  }
}
