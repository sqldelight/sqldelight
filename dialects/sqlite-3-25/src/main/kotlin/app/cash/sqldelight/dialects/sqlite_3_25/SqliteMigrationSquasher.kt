package app.cash.sqldelight.dialects.sqlite_3_25

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.alteredTable
import app.cash.sqldelight.dialects.sqlite_3_25.grammar.psi.SqliteAlterTableRules
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

internal class SqliteMigrationSquasher(
  private val parentSquasher: MigrationSquasher,
) : MigrationSquasher by parentSquasher {
  override fun squish(
    alterTableRules: SqlAlterTableRules,
    into: SqlFileBase,
  ): String {
    if (alterTableRules !is SqliteAlterTableRules) return parentSquasher.squish(alterTableRules, into)
    return when {
      alterTableRules.alterTableRenameColumn != null -> {
        val columnName = PsiTreeUtil.getChildOfType(alterTableRules.alterTableRenameColumn, SqlColumnName::class.java)!!
        val column = alterTableRules.alteredTable(into).columnDefList.map { it.columnName }.single { it.textMatches(columnName.text) }
        val columnAlias = PsiTreeUtil.getChildOfType(alterTableRules.alterTableRenameColumn, SqlColumnAlias::class.java)!!
        into.text.replaceRange(column.startOffset until column.endOffset, columnAlias.text)
      }
      else -> parentSquasher.squish(alterTableRules, into)
    }
  }
}
