package app.cash.sqldelight.dialects.sqlite.json.module

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

class JsonModule : SqlDelightModule {
  override fun typeResolver(parentResolver: TypeResolver): TypeResolver =
    JsonTypeResolver(parentResolver)
}

private class JsonTypeResolver(private val parentResolver: TypeResolver) :
  TypeResolver by parentResolver {
  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    when (functionExpr.functionName.text) {
      "json_array", "json", "json_insert", "json_replace", "json_set", "json_object", "json_patch",
      "json_remove", "json_type", "json_quote", "json_group_array", "json_group_object" ->
        return IntermediateType(PrimitiveType.TEXT)
      "json_array_length" -> return IntermediateType(PrimitiveType.INTEGER)
      "json_extract" -> return IntermediateType(PrimitiveType.TEXT).asNullable()
      "json_valid" -> return IntermediateType(PrimitiveType.BOOLEAN)
    }
    return parentResolver.functionType(functionExpr)
  }
}
