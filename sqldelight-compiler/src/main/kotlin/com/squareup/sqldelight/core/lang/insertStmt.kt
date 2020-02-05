package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.squareup.sqldelight.core.lang.util.childOfType

fun SqliteInsertStmt.acceptsTableInterface(): Boolean {
  return insertStmtValues?.childOfType(SqliteTypes.BIND_EXPR) != null
}