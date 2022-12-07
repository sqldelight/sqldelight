package app.cash.sqldelight.dialects.sqlite_3_35

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.alteredTable
import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteAlterTableRules
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.intellij.psi.util.PsiTreeUtil

internal class SqliteMigrationSquasher(
  private val parentSquasher: MigrationSquasher,
) : MigrationSquasher by parentSquasher {
  override fun squish(
    alterTableRules: SqlAlterTableRules,
    into: SqlFileBase,
  ): String {
    if (alterTableRules !is SqliteAlterTableRules) return parentSquasher.squish(alterTableRules, into)
    return when {
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
