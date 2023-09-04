package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDistinctOnExpr
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlCompoundSelectStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal abstract class DistinctOnExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node), PostgreSqlDistinctOnExpr {

  private val distinctOnColumns get() = children.filterIsInstance<SqlResultColumn>()

  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    return (parent as SqlSelectStmt).queryExposed()
  }

  // Some idea of the basic validation finds the ORDER BY columns in the DISTINCT ON
  // https://github.com/cockroachdb/cockroach/blob/b994d025c678f495cb8b93044e35a8c59595bd78/pkg/sql/opt/optbuilder/distinct.go#L87
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)

    val orderByTerms = (parent.parent as SqlCompoundSelectStmtImpl).orderingTermList

    val orderByColumnNames =
      orderByTerms.mapNotNull { PsiTreeUtil.findChildOfType(it, SqlColumnName::class.java) }

    val distinctOnColumnNames =
      distinctOnColumns.mapNotNull { PsiTreeUtil.findChildOfType(it, SqlColumnName::class.java) }

    orderByColumnNames.zip(distinctOnColumnNames) { orderByCol, _ ->
      if (distinctOnColumnNames.none { distinctOnCol -> distinctOnCol.textMatches(orderByCol) }) {
        annotationHolder.createErrorAnnotation(
          element = orderByCol,
          message = "SELECT DISTINCT ON expressions must match initial ORDER BY expressions",
        )
      }
    }
  }
}
