package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.alecstrong.sql.psi.core.psi.SqlWithClauseAuxiliaryStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlUpdateStmtLimitedImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class UpdateStmtLimitedMixin(
  node: ASTNode,
) : SqlUpdateStmtLimitedImpl(node),
  PostgreSqlUpdateStmtLimited,
  FromQuery {
  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    if (child != joinClause && joinClause != null) {
      return super.queryAvailable(child) +
        joinClause!!.queryExposed().map { it.copy(adjacent = true) }
    }
    return super.queryAvailable(child)
  }

  override fun fromQuery(): Collection<QueryElement.QueryResult> {
    joinClause?.let {
      return it.queryExposed()
    }
    return emptyList()
  }

  override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
    val tablesAvailable = super.tablesAvailable(child)
    val withClauseAuxiliaryStmts = parent as? SqlWithClauseAuxiliaryStmt ?: return tablesAvailable
    val withClause = withClauseAuxiliaryStmts.parent as SqlWithClause
    return tablesAvailable + withClause.tablesExposed()
  }

  private val joinClause: SqlJoinClause? get() = findChildByClass(SqlJoinClause::class.java)
}
