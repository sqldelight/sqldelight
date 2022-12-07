package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.alteredTable
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableRules
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.intellij.psi.util.PsiTreeUtil

internal class PostgreSqlMigrationSquasher(
  private val parentSquasher: MigrationSquasher,
) : MigrationSquasher by parentSquasher {
  override fun squish(
    alterTableRules: SqlAlterTableRules,
    into: SqlFileBase,
  ): String {
    if (alterTableRules !is PostgreSqlAlterTableRules) return parentSquasher.squish(alterTableRules, into)
    return when {
      alterTableRules.alterTableRenameColumn != null -> {
        val columnName = PsiTreeUtil.getChildOfType(alterTableRules.alterTableRenameColumn, SqlColumnName::class.java)!!
        val column = alterTableRules.alteredTable(into).columnDefList.map { it.columnName }.single { it.textMatches(columnName.text) }
        into.text.replaceRange(column.textRange.startOffset until column.textRange.endOffset, alterTableRules.alterTableRenameColumn!!.alterTableColumnAlias!!.text)
      }
      alterTableRules.alterTableDropColumn != null -> {
        val createTable = alterTableRules.alteredTable(into)
        val columnName = PsiTreeUtil.getChildOfType(alterTableRules.alterTableDropColumn, SqlColumnName::class.java)!!
        into.text.replaceRange(
          createTable.columnDefList.first().textRange.startOffset until createTable.columnDefList.last().textRange.endOffset,
          createTable.columnDefList.filterNot { it.columnName.textMatches(columnName.text) }
            .joinToString(separator = ",\n  ") { it.text },
        )
      }
      else -> parentSquasher.squish(alterTableRules, into)
    }
  }
}
