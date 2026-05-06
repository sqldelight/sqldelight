package app.cash.sqldelight.dialects.sqlite_3_44

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_38.SqliteTypeResolver as Sqlite338TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_44.grammar.psi.SqliteExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr

/*
 * Resolve types for sqlite 3.44 aggregate function expressions (e.g. GroupBy(name, ', ' ORDER BY name).
 * If the aggregate function is a simple function (e.g. GroupBy(name, ', ') then this is resolved by the existing parent resolver.
 */
class SqliteTypeResolver(parentResolver: TypeResolver) : Sqlite338TypeResolver(parentResolver) {

  override fun resolvedType(expr: SqlExpr): IntermediateType {
    if (expr is SqliteExtensionExpr && expr.aggregateFunctionExpr != null) {
      return IntermediateType(PrimitiveType.TEXT)
    }

    return super.resolvedType(expr)
  }
}
