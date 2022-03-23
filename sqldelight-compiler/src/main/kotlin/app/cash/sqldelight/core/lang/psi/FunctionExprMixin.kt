package app.cash.sqldelight.core.lang.psi

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
