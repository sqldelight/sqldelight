package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlInsertStmt
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.mixins.SingleRow
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class SqlInsertExcludedTableMixin(node: ASTNode) : SqlCompositeElementImpl(node) {
  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    val insertStmt = parent.parent as PostgreSqlInsertStmt
    val tableName = insertStmt.tableName
    val table = tablesAvailable(this).first { it.tableName.name == tableName.name }.query

    fun excludedTable(name: String) = QueryElement.QueryResult(
      SingleRow(tableName, name),
      table.columns,
      synthesizedColumns = table.synthesizedColumns,
    )

    return super.queryAvailable(child) + excludedTable("excluded") + excludedTable("EXCLUDED")
  }
}
