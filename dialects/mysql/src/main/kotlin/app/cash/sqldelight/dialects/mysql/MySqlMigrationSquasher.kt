package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlAlterTableRules
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlAlterTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlIndexName
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

class MySqlMigrationSquasher(
  private val parentSquasher: MigrationSquasher
) : MigrationSquasher by parentSquasher {
  override fun squish(
    alterTableRule: SqlAlterTableRules,
    into: SqlFileBase
  ): String {
    if (alterTableRule !is MySqlAlterTableRules) return parentSquasher.squish(alterTableRule, into)
    return when {
      alterTableRule.alterTableAddConstraint != null -> {
        val startIndex = alterTableRule.alteredTable(into).columnDefList.last().endOffset
        into.text.replaceRange(startIndex..startIndex, ",\n${alterTableRule.alterTableAddConstraint!!.tableConstraint.text}")
      }
      alterTableRule.alterTableDropIndex != null -> {
        val indexName = PsiTreeUtil.findChildOfType(alterTableRule.alterTableDropIndex, SqlIndexName::class.java)!!
        val createIndex = into.sqlStmtList!!.stmtList.mapNotNull { it.createIndexStmt }
          .single { it.indexName.textMatches(indexName.text) }
        into.text.removeRange(createIndex.startOffset..createIndex.endOffset)
      }
      else -> parentSquasher.squish(alterTableRule, into)
    }
  }

  private fun SqlAlterTableRules.alteredTable(file: SqlFileBase): SqlCreateTableStmt {
    val tableName = (parent as SqlAlterTableStmt).tableName
    return file.sqlStmtList!!.stmtList.mapNotNull { it.createTableStmt }.single { it.tableName.textMatches(tableName.text) }
  }
}
