package foo

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTypeResolver
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

class FooDialect : SqlDelightDialect by SqliteDialect() {
  override fun typeResolver(parentResolver: TypeResolver) = CustomResolver(parentResolver)

  class CustomResolver(private val parentResolver: TypeResolver) : TypeResolver by SqliteTypeResolver(parentResolver) {
    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
      return when (functionExpr.functionName.text.lowercase()) {
        "foo" -> encapsulatingType(functionExpr.exprList, PrimitiveType.INTEGER)
        else -> parentResolver.functionType(functionExpr)
      }
    }
  }
}
