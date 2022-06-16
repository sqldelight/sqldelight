package foo

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTypeResolver
import com.alecstrong.sql.psi.core.psi.*

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
