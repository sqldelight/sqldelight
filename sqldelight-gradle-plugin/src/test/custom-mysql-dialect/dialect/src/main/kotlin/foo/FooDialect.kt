package foo

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.mysql.MySqlDialect
import app.cash.sqldelight.dialects.mysql.MySqlTypeResolver
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

class FooDialect : SqlDelightDialect by MySqlDialect() {
    override fun typeResolver(parentResolver: TypeResolver) = CustomResolver(MySqlTypeResolver(parentResolver))

    class CustomResolver(private val parentResolver: TypeResolver) : TypeResolver by parentResolver {
        override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
            return when (functionExpr.functionName.text.lowercase()) {
                "inet_aton" -> IntermediateType(TEXT)
                "inet_ntoa" -> IntermediateType(TEXT)
                "inet6_aton" -> IntermediateType(TEXT)
                "inet6_ntoa" -> IntermediateType(TEXT)
                else -> parentResolver.functionType(functionExpr)
            }
        }
    }
}