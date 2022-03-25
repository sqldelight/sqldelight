package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlReturningClause
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

internal abstract class ReturningClauseMixin(node: ASTNode) :
  SqlCompositeElementImpl(node), PostgreSqlReturningClause, FromQuery {

  private val queryExposed = ModifiableFileLazy {
    listOf(
      QueryResult(
        null,
        PsiTreeUtil.findChildrenOfType(this, SqlResultColumn::class.java)
          .flatMap { it.queryExposed().flatMap(QueryResult::columns) }
      )
    )
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)

  override fun fromQuery(): Collection<QueryResult> = emptyList()
}
