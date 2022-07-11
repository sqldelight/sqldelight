package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.lang.util.childOfType
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes

fun SqlInsertStmt.acceptsTableInterface(): Boolean {
  return insertStmtValues?.childOfType(SqlTypes.BIND_EXPR) != null
}
