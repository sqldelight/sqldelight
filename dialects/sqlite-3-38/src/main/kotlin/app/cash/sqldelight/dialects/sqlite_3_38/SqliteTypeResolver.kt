package app.cash.sqldelight.dialects.sqlite_3_38

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_35.SqliteTypeResolver as Sqlite335TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_38.grammar.psi.SqliteExtensionExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
/*
This class extends 3_35 SqliteTypeResolver as we need to call the inherited resolvers for previous dialects
Supports type resolution for path operators (->, ->>) in result columns
 */
class SqliteTypeResolver(parentResolver: TypeResolver) : Sqlite335TypeResolver(parentResolver) {

  override fun resolvedType(expr: SqlExpr): IntermediateType {
    return if (expr is SqliteExtensionExpr) {
      // getJsonExpression is the only sqlite extension expr in sqlite_3_38 grammar
      IntermediateType(TEXT).asNullable()
    } else {
      super.resolvedType(expr)
    }
  }
}
