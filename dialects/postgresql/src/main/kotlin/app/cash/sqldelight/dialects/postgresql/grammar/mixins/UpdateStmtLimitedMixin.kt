package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
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
      return super.queryAvailable(child) + joinClause!!.queryExposed()
    }

    return super.queryAvailable(child)
  }

  override fun fromQuery(): Collection<QueryElement.QueryResult> {
    joinClause?.let {
      return it.queryExposed()
    }
    return emptyList()
  }

  private val joinClause: SqlJoinClause? get() = findChildByClass(SqlJoinClause::class.java)
}
