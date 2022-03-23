package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.dialect.sqlite.SqliteType
import app.cash.sqldelight.core.lang.IntermediateType
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.types.typeResolver
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.impl.SqlFunctionExprImpl
import com.intellij.lang.ASTNode

internal class FunctionExprMixin(node: ASTNode?) : SqlFunctionExprImpl(node) {
  fun argumentType(expr: SqlExpr) = when (functionName.text.toLowerCase()) {
    "instr" -> when (expr) {
      exprList.getOrNull(1) -> IntermediateType(SqliteType.TEXT)
      else -> functionType()
    }
    "ifnull", "coalesce" -> functionType()?.asNullable()
    else -> functionType()
  }

  private fun sqliteFunctionType() = when (functionName.text.toLowerCase()) {
    "printf" -> IntermediateType(SqliteType.TEXT).nullableIf(exprList[0].type().javaType.isNullable)
    "datetime", "julianday", "strftime", "sqlite_compileoption_get", "sqlite_source_id", "sqlite_version" -> {
      IntermediateType(SqliteType.TEXT)
    }
    "changes", "last_insert_rowid", "sqlite_compileoption_used", "total_changes" -> {
      IntermediateType(SqliteType.INTEGER)
    }
    "unicode" -> {
      IntermediateType(SqliteType.INTEGER).nullableIf(exprList.any { it.type().javaType.isNullable })
    }
    "randomblob", "zeroblob" -> IntermediateType(SqliteType.BLOB)
    "total", "bm25" -> IntermediateType(SqliteType.REAL)
    "likelihood", "likely", "unlikely" -> exprList[0].type()
    "highlight", "snippet" -> IntermediateType(SqliteType.TEXT).asNullable()
    "offsets" -> IntermediateType(SqliteType.TEXT).asNullable()
    else -> null
  }

  private fun mySqlFunctionType() = when (functionName.text.toLowerCase()) {
    "greatest" -> encapsulatingType(exprList, SqliteType.INTEGER, SqliteType.REAL, SqliteType.TEXT, SqliteType.BLOB)
    "concat" -> encapsulatingType(exprList, SqliteType.TEXT)
    "last_insert_id" -> IntermediateType(SqliteType.INTEGER)
    "row_count" -> IntermediateType(SqliteType.INTEGER)
    "microsecond", "second", "minute", "hour", "day", "week", "month", "year" -> IntermediateType(SqliteType.INTEGER)
    "sin", "cos", "tan" -> IntermediateType(SqliteType.REAL)
    else -> null
  }

  private fun postgreSqlFunctionType() = when (functionName.text.toLowerCase()) {
    "greatest" -> encapsulatingType(exprList, SqliteType.INTEGER, SqliteType.REAL, SqliteType.TEXT, SqliteType.BLOB)
    "concat" -> encapsulatingType(exprList, SqliteType.TEXT)
    "substring" -> IntermediateType(SqliteType.TEXT).nullableIf(exprList[0].type().javaType.isNullable)
    else -> null
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (parent is SqlResultColumn && typeResolver.functionType(this) == null) {
      annotationHolder.createErrorAnnotation(this, "Unknown function ${functionName.text}")
    }
  }
}
