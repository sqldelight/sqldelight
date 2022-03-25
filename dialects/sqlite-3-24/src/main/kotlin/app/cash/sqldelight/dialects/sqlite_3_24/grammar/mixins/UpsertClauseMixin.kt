package app.cash.sqldelight.dialects.sqlite_3_24.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_24.grammar.psi.SqliteUpsertClause
import app.cash.sqldelight.dialects.sqlite_3_24.grammar.psi.SqliteUpsertConflictTarget
import app.cash.sqldelight.dialects.sqlite_3_24.grammar.psi.SqliteUpsertDoUpdate
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.mixins.SingleRow
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class UpsertClauseMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqliteUpsertClause {

  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    val insertStmt = (this.parent as SqlInsertStmt)
    val tableName = insertStmt.tableName
    val table = tablesAvailable(this).first { it.tableName.name == tableName.name }.query

    if (child is SqliteUpsertConflictTarget) {
      return super.queryAvailable(child)
    }

    if (child is SqliteUpsertDoUpdate) {
      val excludedTable = QueryElement.QueryResult(
        SingleRow(
          tableName, "excluded"
        ),
        table.columns,
        synthesizedColumns = table.synthesizedColumns
      )

      val available = arrayListOf(excludedTable)
      available += super.queryAvailable(child)
      return available
    }

    return super.queryAvailable(child)
  }
}
