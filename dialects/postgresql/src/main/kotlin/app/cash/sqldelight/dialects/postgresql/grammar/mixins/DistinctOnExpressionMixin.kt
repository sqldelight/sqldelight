package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDistinctOnExpr
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlCompoundSelectStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class DistinctOnExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node), PostgreSqlDistinctOnExpr {

  private val distinctOnColumns get() = children.filterIsInstance<SqlResultColumn>()

  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    return (parent as SqlSelectStmt).queryExposed()
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)
    val orderByList = (parent.parent as SqlCompoundSelectStmtImpl).orderingTermList
    // todo leftmost only
    distinctOnColumns.forEachIndexed { idx, col ->
      if (!col.textMatches(orderByList[idx])) {
        annotationHolder.createErrorAnnotation(
          element = col,
          message = "DISTINCT ON expression(s) must match the leftmost ORDER BY expression(s)",
        )
      }
    }
  }
}
