package app.cash.sqldelight.dialects.sqlite_3_35.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteReturningClause
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

internal abstract class ReturningClauseMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqliteReturningClause,
  FromQuery {
  private val queryExposed = ModifiableFileLazy {
    listOf(
      QueryElement.QueryResult(
        null,
        PsiTreeUtil.findChildrenOfType(this, SqlResultColumn::class.java)
          .flatMap { it.queryExposed().flatMap(QueryElement.QueryResult::columns) }
      )
    )
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)

  override fun fromQuery() = emptyList<QueryElement.QueryResult>()
}
