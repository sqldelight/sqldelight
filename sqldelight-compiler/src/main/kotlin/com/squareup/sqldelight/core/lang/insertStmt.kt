package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.squareup.sqldelight.core.lang.util.childOfType

fun SqlInsertStmt.acceptsTableInterface(): Boolean {
  return insertStmtValues?.childOfType(SqlTypes.BIND_EXPR) != null
}