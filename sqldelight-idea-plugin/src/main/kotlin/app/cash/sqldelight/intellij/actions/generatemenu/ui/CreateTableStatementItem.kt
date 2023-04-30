package app.cash.sqldelight.intellij.actions.generatemenu.ui

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt

/**
 * Simple wrapper to support default list item rendering for [SqlCreateTableStmt]
 */
class CreateTableStatementItem(val createTableStmt: SqlCreateTableStmt) {
  override fun toString(): String {
    return createTableStmt.tableName.name
  }
}