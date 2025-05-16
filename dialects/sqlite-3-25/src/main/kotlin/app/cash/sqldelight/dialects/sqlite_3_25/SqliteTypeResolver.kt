package app.cash.sqldelight.dialects.sqlite_3_25

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingTypePreferringKotlin
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteTypeResolver as Sqlite324TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_25.grammar.psi.SqliteExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

open class SqliteTypeResolver(private val parentResolver: TypeResolver) : Sqlite324TypeResolver(parentResolver) {

  override fun resolvedType(expr: SqlExpr): IntermediateType = when (expr) {
    is SqliteExtensionExpr -> {
      functionType(expr.windowFunctionExpr)!! // currently this is the only sqlite extension expr in 3_25
    }
    else -> super.resolvedType(expr)
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.sqliteFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.sqliteFunctionType() = when (functionName.text.lowercase()) {
    "dense_rank", "ntile", "rank", "row_number" -> IntermediateType(INTEGER)
    "cume_dist", "percent_rank" -> IntermediateType(REAL)
    "lag", "lead", "first_value", "last_value", "nth_value", "group_concat" -> encapsulatingTypePreferringKotlin(exprList, INTEGER, REAL, TEXT).asNullable()
    else -> null
  }
}
