package app.cash.sqldelight.dialects.sqlite_3_18

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.sqlite_3_18.psi.SqliteTypeName

open class SqliteTypeResolver(private val parentResolver: TypeResolver) : TypeResolver by parentResolver {
  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.sqliteFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
    check(this is SqliteTypeName) { "Get $this" }
    return when {
      textDataType != null -> IntermediateType(TEXT)
      blobDataType != null -> IntermediateType(BLOB)
      intDataType != null -> IntermediateType(INTEGER)
      realDataType != null -> IntermediateType(REAL)
      else -> throw IllegalArgumentException("Unknown sql type $text")
    }
  }

  private fun SqlFunctionExpr.sqliteFunctionType() = when (functionName.text.toLowerCase()) {
    "printf" -> IntermediateType(TEXT).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
    "datetime", "julianday", "strftime", "sqlite_compileoption_get", "sqlite_source_id", "sqlite_version" -> {
      IntermediateType(TEXT)
    }
    "changes", "last_insert_rowid", "sqlite_compileoption_used", "total_changes" -> {
      IntermediateType(INTEGER)
    }
    "unicode" -> {
      IntermediateType(INTEGER).nullableIf(exprList.any { resolvedType(it).javaType.isNullable })
    }
    "randomblob", "zeroblob" -> IntermediateType(BLOB)
    "total", "bm25" -> IntermediateType(REAL)
    "likelihood", "likely", "unlikely" -> resolvedType(exprList[0])
    "highlight", "snippet" -> IntermediateType(TEXT).asNullable()
    "offsets" -> IntermediateType(TEXT).asNullable()
    else -> null
  }
}
