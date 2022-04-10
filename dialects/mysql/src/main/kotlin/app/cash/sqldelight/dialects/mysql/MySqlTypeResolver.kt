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
    return IntermediateType(
      when {
        approximateNumericDataType != null -> REAL
        binaryDataType != null -> BLOB
        dateDataType != null -> TEXT
        tinyIntDataType != null -> if (tinyIntDataType!!.text == "BOOLEAN") {
          MySqlType.TINY_INT_BOOL
        } else {
          TINY_INT
        }
        smallIntDataType != null -> SMALL_INT
        mediumIntDataType != null -> MySqlType.INTEGER
        intDataType != null -> MySqlType.INTEGER
        bigIntDataType != null -> BIG_INT
        fixedPointDataType != null -> MySqlType.NUMERIC
        jsonDataType != null -> TEXT
        enumSetType != null -> TEXT
        characterType != null -> TEXT
        bitDataType != null -> MySqlType.BIT
        else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
      }
    )
  }
}
