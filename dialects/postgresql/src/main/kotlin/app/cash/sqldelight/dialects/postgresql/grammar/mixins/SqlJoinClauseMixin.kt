package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypes
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.impl.SqlJoinClauseImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

internal open class SqlJoinClauseMixin(node: ASTNode) : SqlJoinClauseImpl(node) {

  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    return if (joinOperatorList
        .flatMap { it.children.toList() }
        .find { it.elementType == PostgreSqlTypes.LATERAL } != null
    ) {
      tableOrSubqueryList.takeWhile { it != child }.flatMap { it.queryExposed() }
    } else {
      super.queryAvailable(child)
    }
  }
}
