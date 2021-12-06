package app.cash.sqldelight.core.dialect.mysql

import app.cash.sqldelight.core.dialect.sqlite.SqliteType
import app.cash.sqldelight.core.lang.IntermediateType
import app.cash.sqldelight.core.lang.util.encapsulatingType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import com.alecstrong.sql.psi.core.mysql.psi.MySqlExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.squareup.kotlinpoet.BOOLEAN

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
