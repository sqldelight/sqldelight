package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import com.alecstrong.sql.psi.core.postgresql.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName

class PostgreSqlTypeResolver(private val parentResolver: TypeResolver) : TypeResolver by parentResolver {
  override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
    check(this is PostgreSqlTypeName)
    return when {
      smallIntDataType != null -> IntermediateType(PostgreSqlType.SMALL_INT)
      intDataType != null -> IntermediateType(PostgreSqlType.INTEGER)
      bigIntDataType != null -> IntermediateType(PostgreSqlType.BIG_INT)
      numericDataType != null -> IntermediateType(REAL)
      approximateNumericDataType != null -> IntermediateType(REAL)
      stringDataType != null -> IntermediateType(TEXT)
      smallSerialDataType != null -> IntermediateType(PostgreSqlType.SMALL_INT)
      serialDataType != null -> IntermediateType(PostgreSqlType.INTEGER)
      bigSerialDataType != null -> IntermediateType(PostgreSqlType.BIG_INT)
      dateDataType != null -> IntermediateType(TEXT)
      jsonDataType != null -> IntermediateType(TEXT)
      else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
    }
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.postgreSqlFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.postgreSqlFunctionType() = when (functionName.text.toLowerCase()) {
    "greatest" -> encapsulatingType(exprList, PrimitiveType.INTEGER, REAL, TEXT, BLOB)
    "concat" -> encapsulatingType(exprList, TEXT)
    "substring" -> IntermediateType(TEXT).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
    else -> null
  }
}
