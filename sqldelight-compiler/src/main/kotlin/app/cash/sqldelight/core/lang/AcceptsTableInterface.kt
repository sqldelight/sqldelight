package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.lang.util.childOfType
import app.cash.sqldelight.core.lang.util.table
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes

fun SqlInsertStmt.shouldInferColumns() = insertStmtValues?.childOfType(SqlTypes.BIND_EXPR) != null && columnNameList.isNotEmpty()

fun SqlInsertStmt.acceptsTableInterface(): Boolean = if (shouldInferColumns()) {
  // It is safe to just compare the sizes because sql-psi already did check the references or default checks.
  columnNameList.size == table.query.columns.size
} else {
  insertStmtValues?.childOfType(SqlTypes.BIND_EXPR) != null
}
