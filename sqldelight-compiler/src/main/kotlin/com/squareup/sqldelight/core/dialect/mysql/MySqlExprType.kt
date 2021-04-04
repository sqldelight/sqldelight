package com.squareup.sqldelight.core.dialect.mysql

import com.alecstrong.sql.psi.core.mysql.psi.MySqlExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.sqldelight.core.dialect.sqlite.SqliteType
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.util.encapsulatingType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType

internal fun MySqlExtensionExpr.type(): IntermediateType {
  // if (expr, expr, expr)
  return encapsulatingType(
    ifExpr.findChildrenOfType<SqlExpr>().drop(1),
    SqliteType.INTEGER, SqliteType.REAL, SqliteType.TEXT, SqliteType.BLOB
  )
}

internal fun MySqlExtensionExpr.argumentType(expr: SqlExpr): IntermediateType {
  return if (expr == ifExpr.children[0]) IntermediateType(SqliteType.INTEGER, BOOLEAN)
  else IntermediateType(SqliteType.ARGUMENT)
}
