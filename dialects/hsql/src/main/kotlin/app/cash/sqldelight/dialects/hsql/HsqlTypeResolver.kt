package app.cash.sqldelight.dialects.hsql

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialects.hsql.HsqlType.BIG_INT
import app.cash.sqldelight.dialects.hsql.HsqlType.SMALL_INT
import app.cash.sqldelight.dialects.hsql.HsqlType.TINY_INT
import app.cash.sqldelight.dialects.hsql.grammar.psi.HsqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName

class HsqlTypeResolver(private val parentResolver: TypeResolver) : TypeResolver by parentResolver {
  override fun definitionType(typeName: SqlTypeName): IntermediateType {
    check(typeName is HsqlTypeName)
    with(typeName) {
      return when {
        approximateNumericDataType != null -> IntermediateType(PrimitiveType.REAL)
        binaryStringDataType != null -> IntermediateType(PrimitiveType.BLOB)
        dateDataType != null -> IntermediateType(PrimitiveType.TEXT)
        tinyIntDataType != null -> IntermediateType(HsqlType.TINY_INT)
        smallIntDataType != null -> IntermediateType(HsqlType.SMALL_INT)
        intDataType != null -> IntermediateType(HsqlType.INTEGER)
        bigIntDataType != null -> IntermediateType(HsqlType.BIG_INT)
        fixedPointDataType != null -> IntermediateType(PrimitiveType.INTEGER)
        characterStringDataType != null -> IntermediateType(PrimitiveType.TEXT)
        booleanDataType != null -> IntermediateType(HsqlType.BOOL)
        bitStringDataType != null -> IntermediateType(PrimitiveType.BLOB)
        intervalDataType != null -> IntermediateType(PrimitiveType.BLOB)
        else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${typeName.text}")
      }
    }
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.hsqlFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.hsqlFunctionType() = when (functionName.text.toLowerCase()) {
    "coalesce", "ifnull" -> encapsulatingType(exprList, TINY_INT, SMALL_INT, HsqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB)
    "max" -> encapsulatingType(exprList, TINY_INT, SMALL_INT, HsqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB).asNullable()
    "min" -> encapsulatingType(exprList, BLOB, TEXT, TINY_INT, SMALL_INT, INTEGER, HsqlType.INTEGER, BIG_INT, REAL).asNullable()
    else -> null
  }
}
