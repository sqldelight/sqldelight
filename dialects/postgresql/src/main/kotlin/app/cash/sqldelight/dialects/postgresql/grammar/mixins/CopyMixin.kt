package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlCopyStdin
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class CopyMixin(node: ASTNode) : SqlCompositeElementImpl(node), PostgreSqlCopyStdin {
  override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
    val tables = tablesAvailable(child)
    return tables.map {
      it.query
    }
  }
}
