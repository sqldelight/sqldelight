package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterSequenceStmt
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class AlterSequenceMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlAlterSequenceStmt {
  // Query any OWNED BY tableName element to allow the columnName to be resolved
  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    return tablesAvailable(child).map { it.query }
  }
}
