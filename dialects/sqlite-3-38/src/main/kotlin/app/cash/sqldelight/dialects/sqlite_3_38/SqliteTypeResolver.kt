package app.cash.sqldelight.dialects.sqlite_3_38

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_37.SqliteTypeResolver as Sqlite337TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_38.grammar.psi.SqliteExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
/*
This class extends 3_37 SqliteTypeResolver as we need to call the inherited resolvers for previous dialects
Supports type resolution for path operators (->, ->>) in result columns
 */
open class SqliteTypeResolver(parentResolver: TypeResolver) : Sqlite337TypeResolver(parentResolver) {

  override fun resolvedType(expr: SqlExpr): IntermediateType {
    return if (expr is SqliteExtensionExpr) {
      // getJsonExpression is the only sqlite extension expr in sqlite_3_38 grammar
      IntermediateType(TEXT).asNullable()
    } else {
      super.resolvedType(expr)
    }
  }
}
