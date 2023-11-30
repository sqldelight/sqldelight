package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlInsertStmt
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.SqlWithClause
import com.alecstrong.sql.psi.core.psi.SqlWithClauseAuxiliaryStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlInsertStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class SqlInsertStmtMixin(
  node: ASTNode,
) : SqlInsertStmtImpl(node), PostgreSqlInsertStmt {

  override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
    val tablesAvailable = super.tablesAvailable(child)
    val withClauseAuxiliaryStmts = parent as? SqlWithClauseAuxiliaryStmt ?: return tablesAvailable
    val withClause = withClauseAuxiliaryStmts.parent as SqlWithClause
    return tablesAvailable + withClause.tablesExposed()
  }
}
