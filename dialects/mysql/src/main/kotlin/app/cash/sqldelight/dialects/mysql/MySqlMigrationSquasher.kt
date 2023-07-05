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

internal class MySqlMigrationSquasher(
  private val parentSquasher: MigrationSquasher,
) : MigrationSquasher by parentSquasher {
  override fun squish(
    alterTableRules: SqlAlterTableRules,
    into: SqlFileBase,
  ): String {
    if (alterTableRules !is MySqlAlterTableRules) return parentSquasher.squish(alterTableRules, into)
    return when {
      alterTableRules.alterTableAddConstraint != null -> {
        val startIndex = alterTableRules.alteredTable(into).columnDefList.last().textRange.endOffset
        into.text.replaceRange(startIndex until startIndex, ",\n  ${alterTableRules.alterTableAddConstraint!!.tableConstraint.text}")
      }
      alterTableRules.alterTableAddIndex != null -> {
        val startIndex = alterTableRules.alteredTable(into).columnDefList.last().textRange.endOffset
        val constraint = alterTableRules.alterTableAddIndex!!.text.substringAfter("ADD").trim()
        into.text.replaceRange(startIndex until startIndex, ",\n  $constraint")
      }
      alterTableRules.alterTableDropIndex != null -> {
        val indexName = PsiTreeUtil.findChildOfType(alterTableRules.alterTableDropIndex, SqlIndexName::class.java)!!
        val createIndex = into.sqlStmtList!!.stmtList.mapNotNull { it.createIndexStmt }
          .single { it.indexName.textMatches(indexName.text) }
        into.text.removeRange(createIndex.textRange.startOffset..createIndex.textRange.endOffset)
      }
      alterTableRules.alterTableAddColumn != null -> {
        val placement = alterTableRules.alterTableAddColumn!!.placementClause
        val columnDef = PsiTreeUtil.getChildOfType(alterTableRules.alterTableAddColumn!!, SqlColumnDef::class.java)!!
        into.text.replaceWithPlacement(alterTableRules.alteredTable(into), placement, columnDef)
      }
      alterTableRules.alterTableChangeColumn != null -> {
        val placement = alterTableRules.alterTableChangeColumn!!.placementClause
        val columnDef = PsiTreeUtil.getChildOfType(alterTableRules.alterTableChangeColumn!!, SqlColumnDef::class.java)!!
        val columnName = PsiTreeUtil.getChildOfType(alterTableRules.alterTableChangeColumn!!, SqlColumnName::class.java)!!
        into.text.replaceWithPlacement(alterTableRules.alteredTable(into), placement, columnDef, columnName)
      }
      alterTableRules.alterTableModifyColumn != null -> {
        val placement = alterTableRules.alterTableModifyColumn!!.placementClause
        val columnDef = PsiTreeUtil.getChildOfType(alterTableRules.alterTableModifyColumn!!, SqlColumnDef::class.java)!!
        val columnName = columnDef.columnName
        into.text.replaceWithPlacement(alterTableRules.alteredTable(into), placement, columnDef, columnName)
      }
      alterTableRules.alterTableDropColumn != null -> {
        val columnName = PsiTreeUtil.getChildOfType(alterTableRules.alterTableDropColumn, SqlColumnName::class.java)!!
        into.text.replaceWithPlacement(alterTableRules.alteredTable(into), null, null, columnName)
      }
      alterTableRules.alterTableConvertCharacterSet != null -> {
        val startIndex = alterTableRules.alteredTable(into).textRange.endOffset
        val rule = alterTableRules.alterTableConvertCharacterSet!!.text.substringAfter("TO")
        into.text.replaceRange(startIndex until startIndex, rule)
      }
      alterTableRules.rowFormatClause != null -> {
        val startIndex = alterTableRules.alteredTable(into).textRange.endOffset
        into.text.replaceRange(startIndex until startIndex, " ${alterTableRules.rowFormatClause!!.text}")
      }
      else -> parentSquasher.squish(alterTableRules, into)
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
      createTableStmt.columnDefList.first().textRange.startOffset until createTableStmt.columnDefList.last().textRange.endOffset,
      columnDefs.joinToString(separator = ",\n  ") { it.text },
    )
  }
}
