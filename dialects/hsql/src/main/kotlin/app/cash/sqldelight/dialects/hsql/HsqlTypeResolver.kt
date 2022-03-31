package app.cash.sqldelight.dialects.hsql

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.hsql.grammar.psi.HsqlTypeName
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
}
