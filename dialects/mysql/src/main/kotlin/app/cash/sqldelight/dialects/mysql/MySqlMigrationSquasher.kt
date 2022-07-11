package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.dialect.api.MigrationSquasher
import app.cash.sqldelight.dialect.api.alteredTable
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlAlterTableRules
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlPlacementClause
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlIndexName
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

internal class MySqlMigrationSquasher(
  private val parentSquasher: MigrationSquasher,
) : MigrationSquasher by parentSquasher {
  override fun squish(
    alterTableRule: SqlAlterTableRules,
    into: SqlFileBase,
  ): String {
    if (alterTableRule !is MySqlAlterTableRules) return parentSquasher.squish(alterTableRule, into)
    return when {
      alterTableRule.alterTableAddConstraint != null -> {
        val startIndex = alterTableRule.alteredTable(into).columnDefList.last().endOffset
        into.text.replaceRange(startIndex until startIndex, ",\n  ${alterTableRule.alterTableAddConstraint!!.tableConstraint.text}")
      }
      alterTableRule.alterTableAddIndex != null -> {
        val startIndex = alterTableRule.alteredTable(into).columnDefList.last().endOffset
        val constraint = alterTableRule.alterTableAddIndex!!.text.substringAfter("ADD").trim()
        into.text.replaceRange(startIndex until startIndex, ",\n  $constraint")
      }
      alterTableRule.alterTableDropIndex != null -> {
        val indexName = PsiTreeUtil.findChildOfType(alterTableRule.alterTableDropIndex, SqlIndexName::class.java)!!
        val createIndex = into.sqlStmtList!!.stmtList.mapNotNull { it.createIndexStmt }
          .single { it.indexName.textMatches(indexName.text) }
        into.text.removeRange(createIndex.startOffset..createIndex.endOffset)
      }
      alterTableRule.alterTableAddColumn != null -> {
        val placement = alterTableRule.alterTableAddColumn!!.placementClause
        val columnDef = PsiTreeUtil.getChildOfType(alterTableRule.alterTableAddColumn!!, SqlColumnDef::class.java)!!
        into.text.replaceWithPlacement(alterTableRule.alteredTable(into), placement, columnDef)
      }
      alterTableRule.alterTableChangeColumn != null -> {
        val placement = alterTableRule.alterTableChangeColumn!!.placementClause
        val columnDef = PsiTreeUtil.getChildOfType(alterTableRule.alterTableChangeColumn!!, SqlColumnDef::class.java)!!
        val columnName = PsiTreeUtil.getChildOfType(alterTableRule.alterTableChangeColumn!!, SqlColumnName::class.java)!!
        into.text.replaceWithPlacement(alterTableRule.alteredTable(into), placement, columnDef, columnName)
      }
      alterTableRule.alterTableModifyColumn != null -> {
        val placement = alterTableRule.alterTableModifyColumn!!.placementClause
        val columnDef = PsiTreeUtil.getChildOfType(alterTableRule.alterTableModifyColumn!!, SqlColumnDef::class.java)!!
        val columnName = columnDef.columnName
        into.text.replaceWithPlacement(alterTableRule.alteredTable(into), placement, columnDef, columnName)
      }
      alterTableRule.alterTableDropColumn != null -> {
        val columnName = PsiTreeUtil.getChildOfType(alterTableRule.alterTableDropColumn, SqlColumnName::class.java)!!
        into.text.replaceWithPlacement(alterTableRule.alteredTable(into), null, null, columnName)
      }
      alterTableRule.alterTableConvertCharacterSet != null -> {
        val startIndex = alterTableRule.alteredTable(into).endOffset
        val rule = alterTableRule.alterTableConvertCharacterSet!!.text.substringAfter("TO")
        into.text.replaceRange(startIndex until startIndex, rule)
      }
      alterTableRule.rowFormatClause != null -> {
        val startIndex = alterTableRule.alteredTable(into).endOffset
        into.text.replaceRange(startIndex until startIndex, " ${alterTableRule.rowFormatClause!!.text}")
      }
      else -> parentSquasher.squish(alterTableRule, into)
    }
  }

  private fun String.replaceWithPlacement(
    createTableStmt: SqlCreateTableStmt,
    placementClause: MySqlPlacementClause?,
    columnDef: SqlColumnDef?,
    replace: SqlColumnName? = null,
  ): String {
    val columnDefs = createTableStmt.columnDefList.toMutableList()

    if (replace != null) {
      columnDefs.removeIf { it.columnName.textMatches(replace.text) }
    }

    if (columnDef != null) {
      if (placementClause == null) {
        columnDefs += columnDef
      } else if (placementClause.node.getChildren(null).first().text == "FIRST") {
        columnDefs.add(0, columnDef)
      } else {
        val columnName = PsiTreeUtil.getChildOfType(placementClause, SqlColumnName::class.java)!!
        columnDefs.add(
          columnDefs.indexOfFirst { it.columnName.textMatches(columnName.text) } + 1,
          columnDef,
        )
      }
    }

    return this.replaceRange(
      createTableStmt.columnDefList.first().startOffset until createTableStmt.columnDefList.last().endOffset,
      columnDefs.joinToString(separator = ",\n  ") { it.text },
    )
  }
}
