package app.cash.sqldelight.dialects.mysql

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.ARGUMENT
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialect.api.encapsulatingTypePreferringKotlin
import app.cash.sqldelight.dialects.mysql.MySqlType.BIG_INT
import app.cash.sqldelight.dialects.mysql.MySqlType.SMALL_INT
import app.cash.sqldelight.dialects.mysql.MySqlType.TINY_INT
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlExtensionExpr
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlBinaryAddExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryMultExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryPipeExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

class MySqlTypeResolver(
  private val parentResolver: TypeResolver,
) : TypeResolver by parentResolver {
  override fun resolvedType(expr: SqlExpr): IntermediateType {
    return when (expr) {
      is MySqlExtensionExpr -> encapsulatingType(
        PsiTreeUtil.findChildrenOfType(expr.ifExpr, SqlExpr::class.java).drop(1),
        TINY_INT,
        SMALL_INT,
        MySqlType.INTEGER,
        INTEGER,
        BIG_INT,
        REAL,
        MySqlType.TIMESTAMP,
        MySqlType.DATE,
        MySqlType.DATETIME,
        MySqlType.TIME,
        TEXT,
        BLOB,
      )
      is SqlBinaryExpr -> {
        if (expr.childOfType(
            TokenSet.create(
              SqlTypes.EQ, SqlTypes.EQ2, SqlTypes.NEQ,
              SqlTypes.NEQ2, SqlTypes.AND, SqlTypes.OR, SqlTypes.GT, SqlTypes.GTE,
              SqlTypes.LT, SqlTypes.LTE,
            ),
          ) != null
        ) {
          IntermediateType(PrimitiveType.BOOLEAN)
        } else {
          encapsulatingType(
            exprList = expr.getExprList(),
            nullability = { exprListNullability ->
              (expr is SqlBinaryAddExpr || expr is SqlBinaryMultExpr || expr is SqlBinaryPipeExpr) &&
                exprListNullability.any { it }
            },
            TINY_INT,
            SMALL_INT,
            MySqlType.INTEGER,
            INTEGER,
            BIG_INT,
            REAL,
            TEXT,
            BLOB,
          )
        }
      }
      else -> parentResolver.resolvedType(expr)
    }
  }

  override fun argumentType(parent: PsiElement, argument: SqlExpr): IntermediateType {
    when (parent) {
      is MySqlExtensionExpr -> {
        return if (argument == parent.ifExpr?.children?.first()) {
          IntermediateType(PrimitiveType.BOOLEAN)
        } else {
          IntermediateType(ARGUMENT)
        }
      }
    }
    return parentResolver.argumentType(parent, argument)
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.mySqlFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.mySqlFunctionType() = when (functionName.text.lowercase()) {
    "greatest" -> encapsulatingTypePreferringKotlin(
      exprList,
      TINY_INT,
      SMALL_INT,
      MySqlType.INTEGER,
      INTEGER,
      BIG_INT,
      REAL,
      MySqlType.TIMESTAMP,
      MySqlType.DATE,
      MySqlType.DATETIME,
      MySqlType.TIME,
      TEXT,
      BLOB,
    )
    "least" -> encapsulatingTypePreferringKotlin(
      exprList,
      BLOB,
      TEXT,
      MySqlType.TIME,
      MySqlType.DATETIME,
      MySqlType.DATE,
      MySqlType.TIMESTAMP,
      TINY_INT,
      SMALL_INT,
      INTEGER,
      MySqlType.INTEGER,
      BIG_INT,
      REAL,
    )
    "concat" -> encapsulatingType(exprList, TEXT)
    "last_insert_id" -> IntermediateType(INTEGER)
    "row_count" -> IntermediateType(INTEGER)
    "microsecond", "second", "minute", "hour", "day", "week", "month", "year" -> IntermediateType(
      INTEGER,
    )
    "sin", "cos", "tan" -> IntermediateType(REAL)
    "coalesce", "ifnull" -> encapsulatingTypePreferringKotlin(exprList, TINY_INT, SMALL_INT, MySqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB, nullability = { exprListNullability ->
      exprListNullability.all { it }
    })
    "max" -> encapsulatingTypePreferringKotlin(
      exprList,
      TINY_INT,
      SMALL_INT,
      MySqlType.INTEGER,
      INTEGER,
      BIG_INT,
      REAL,
      MySqlType.TIMESTAMP,
      MySqlType.DATE,
      MySqlType.DATETIME,
      MySqlType.TIME,
      TEXT,
      BLOB,
    ).asNullable()
    "min" -> encapsulatingTypePreferringKotlin(
      exprList,
      BLOB,
      TEXT,
      MySqlType.TIME,
      MySqlType.DATETIME,
      MySqlType.DATE,
      MySqlType.TIMESTAMP,
      TINY_INT,
      SMALL_INT,
      INTEGER,
      MySqlType.INTEGER,
      BIG_INT,
      REAL,
    ).asNullable()
    "sum" -> {
      val type = resolvedType(exprList.single())
      if (type.dialectType == REAL) {
        IntermediateType(REAL).asNullable()
      } else {
        IntermediateType(INTEGER).asNullable()
      }
    }
    "unix_timestamp" -> IntermediateType(TEXT)
    "to_seconds" -> IntermediateType(INTEGER)
    "json_arrayagg" -> IntermediateType(TEXT)
    "date_add", "date_sub" -> IntermediateType(TEXT)
    "now" -> IntermediateType(TEXT)
    "char_length", "character_length" -> IntermediateType(INTEGER).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
    else -> null
  }

  override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
    check(this is MySqlTypeName)
    return IntermediateType(
      when {
        approximateNumericDataType != null -> REAL
        binaryDataType != null -> BLOB
        dateDataType != null -> {
          when (dateDataType!!.firstChild.text.uppercase()) {
            "DATE" -> MySqlType.DATE
            "TIME" -> MySqlType.TIME
            "DATETIME" -> MySqlType.DATETIME
            "TIMESTAMP" -> MySqlType.TIMESTAMP
            "YEAR" -> TEXT
            else -> throw IllegalArgumentException("Unknown date type ${dateDataType!!.text}")
          }
        }
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
      },
    )
  }
}

private fun PsiElement.childOfType(types: TokenSet): PsiElement? {
  return node.findChildByType(types)?.psi
}
