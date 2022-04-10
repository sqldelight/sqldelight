package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.core.dialect.mysql.MySqlType
import app.cash.sqldelight.core.dialect.mysql.MySqlType.BIG_INT
import app.cash.sqldelight.core.dialect.mysql.MySqlType.SMALL_INT
import app.cash.sqldelight.core.dialect.mysql.MySqlType.TINY_INT
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.ARGUMENT
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlExtensionExpr
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

internal class MySqlTypeResolver(
  private val parentResolver: TypeResolver
) : TypeResolver by parentResolver {
  override fun resolvedType(expr: SqlExpr): IntermediateType {
    return when (expr) {
      is MySqlExtensionExpr -> encapsulatingType(
        PsiTreeUtil.findChildrenOfType(expr.ifExpr, SqlExpr::class.java).drop(1),
        INTEGER, REAL, TEXT, BLOB
      )
      else -> parentResolver.resolvedType(expr)
    }
  }

  override fun argumentType(parent: PsiElement, argument: SqlExpr): IntermediateType {
    when (parent) {
      is MySqlExtensionExpr -> {
        return if (argument == parent.ifExpr?.children?.first()) IntermediateType(PrimitiveType.BOOLEAN)
        else IntermediateType(ARGUMENT)
      }
    }
    return parentResolver.argumentType(parent, argument)
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.mySqlFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.mySqlFunctionType() = when (functionName.text.toLowerCase()) {
    "greatest" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
    "concat" -> encapsulatingType(exprList, TEXT)
    "last_insert_id" -> IntermediateType(INTEGER)
    "row_count" -> IntermediateType(INTEGER)
    "microsecond", "second", "minute", "hour", "day", "week", "month", "year" -> IntermediateType(
      INTEGER
    )
    "sin", "cos", "tan" -> IntermediateType(REAL)
    "coalesce", "ifnull" -> encapsulatingType(exprList, TINY_INT, SMALL_INT, MySqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB)
    "max" -> encapsulatingType(exprList, TINY_INT, SMALL_INT, MySqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB).asNullable()
    "min" -> encapsulatingType(exprList, BLOB, TEXT, TINY_INT, SMALL_INT, INTEGER, MySqlType.INTEGER, BIG_INT, REAL).asNullable()
    "unix_timestamp" -> IntermediateType(TEXT)
    "to_seconds" -> IntermediateType(INTEGER)
    "json_arrayagg" -> IntermediateType(TEXT)
    else -> null
  }

  override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
    check(this is MySqlTypeName)
    return when {
      approximateNumericDataType != null -> IntermediateType(REAL)
      binaryDataType != null -> IntermediateType(BLOB)
      dateDataType != null -> IntermediateType(TEXT)
      tinyIntDataType != null -> if (tinyIntDataType!!.text == "BOOLEAN") {
        IntermediateType(MySqlType.TINY_INT_BOOL)
      } else {
        IntermediateType(MySqlType.TINY_INT)
      }
      smallIntDataType != null -> IntermediateType(MySqlType.SMALL_INT)
      mediumIntDataType != null -> IntermediateType(MySqlType.INTEGER)
      intDataType != null -> IntermediateType(MySqlType.INTEGER)
      bigIntDataType != null -> IntermediateType(MySqlType.BIG_INT)
      fixedPointDataType != null -> IntermediateType(INTEGER)
      jsonDataType != null -> IntermediateType(TEXT)
      enumSetType != null -> IntermediateType(TEXT)
      characterType != null -> IntermediateType(TEXT)
      bitDataType != null -> IntermediateType(MySqlType.BIT)
      else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
    }
  }
}
