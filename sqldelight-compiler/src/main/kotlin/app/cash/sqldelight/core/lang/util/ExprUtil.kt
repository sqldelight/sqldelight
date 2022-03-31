/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.core.lang.util

import app.cash.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import app.cash.sqldelight.core.lang.types.typeResolver
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.ARGUMENT
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.NULL
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import com.alecstrong.sql.psi.core.psi.SqlBetweenExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryAddExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryLikeExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryMultExpr
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlCaseExpr
import com.alecstrong.sql.psi.core.psi.SqlCastExpr
import com.alecstrong.sql.psi.core.psi.SqlCollateExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlExistsExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlInExpr
import com.alecstrong.sql.psi.core.psi.SqlIsExpr
import com.alecstrong.sql.psi.core.psi.SqlLiteralExpr
import com.alecstrong.sql.psi.core.psi.SqlNullExpr
import com.alecstrong.sql.psi.core.psi.SqlOtherExpr
import com.alecstrong.sql.psi.core.psi.SqlParenExpr
import com.alecstrong.sql.psi.core.psi.SqlRaiseExpr
import com.alecstrong.sql.psi.core.psi.SqlSetterExpression
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlUnaryExpr
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

internal val SqlExpr.name: String get() = when (this) {
  is SqlCastExpr -> expr.name
  is SqlParenExpr -> expr?.name ?: "value"
  is SqlFunctionExpr -> functionName.text
  is SqlColumnExpr -> allocateName(columnName)
  else -> "expr"
}

internal class AnsiSqlTypeResolver : TypeResolver {
  override fun resolvedType(expr: SqlExpr): IntermediateType {
    return expr.ansiType()
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.typeReturned()
  }

  override fun argumentType(bindArg: SqlBindExpr): IntermediateType {
    return bindArg.inferredType().copy(bindArg = bindArg)
  }

  override fun definitionType(typeName: SqlTypeName) =
    throw UnsupportedOperationException("ANSI SQL is not supported for being used as a dialect.")

  override fun argumentType(
    parent: PsiElement,
    argument: SqlExpr
  ): IntermediateType {
    return when (parent) {
      is SqlExpr -> parent.argumentType(argument)
      is SqlSetterExpression -> parent.argumentType()
      else -> throw IllegalStateException("Cannot infer argument type for $parent")
    }
  }

  private fun SqlFunctionExpr.typeReturned() = when (functionName.text.toLowerCase()) {
    "round" -> {
      // Single arg round function returns an int. Otherwise real.
      if (exprList.size == 1) {
        IntermediateType(INTEGER).nullableIf(exprList[0].type().javaType.isNullable)
      } else {
        IntermediateType(REAL).nullableIf(exprList.any { it.type().javaType.isNullable })
      }
    }

    /**
     * sum's output is always nullable because it returns NULL for an input that's empty or only contains NULLs.
     *
     * https://www.sqlite.org/lang_aggfunc.html#sumunc
     * >>> The result of sum() is an integer value if all non-NULL inputs are integers. If any input to sum() is neither
     * >>> an integer or a NULL then sum() returns a floating point value which might be an approximation to the true sum.
     *
     */
    "sum" -> {
      val type = exprList[0].type()
      if (type.dialectType == INTEGER && !type.javaType.isNullable) {
        type.asNullable()
      } else {
        IntermediateType(REAL).asNullable()
      }
    }

    "lower", "ltrim", "replace", "rtrim", "substr", "trim", "upper", "group_concat" -> {
      IntermediateType(TEXT).nullableIf(exprList[0].type().javaType.isNullable)
    }

    "date", "time", "char", "hex", "quote", "soundex", "typeof" -> {
      IntermediateType(TEXT)
    }

    "random", "count" -> {
      IntermediateType(INTEGER)
    }

    "instr", "length" -> {
      IntermediateType(INTEGER).nullableIf(exprList.any { it.type().javaType.isNullable })
    }

    "avg" -> IntermediateType(REAL).asNullable()
    "abs" -> exprList[0].type()
    "coalesce", "ifnull" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
    "nullif" -> exprList[0].type().asNullable()
    "max" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB).asNullable()
    "min" -> encapsulatingType(exprList, BLOB, TEXT, INTEGER, REAL).asNullable()
    else -> null
  }

  override fun simplifyType(intermediateType: IntermediateType): IntermediateType {
    with (intermediateType) {
      if (javaType != dialectType.javaType
        && javaType.copy(nullable = false, annotations = emptyList()) == dialectType.javaType) {
        // We don't need an adapter for only annotations.
        return copy(simplified = true)
      }
    }

    return intermediateType
  }
}

private fun SqlExpr.type(): IntermediateType {
  return typeResolver.resolvedType(this)
}

/**
 * If javaType is true, this will return a possible more descriptive type for column expressions.
 *
 * Order of operations:
 * expr ::= ( raise_expr
 *          | case_expr
 *          | exists_expr
 *          | in_expr
 *          | between_expr
 *          | is_expr
 *          | null_expr
 *          | like_expr
 *          | collate_expr
 *          | cast_expr
 *          | paren_expr
 *          | function_expr
 *          | binary_expr
 *          | unary_expr
 *          | bind_expr
 *          | literal_expr
 *          | column_expr )
 */
private fun SqlExpr.ansiType(): IntermediateType = when (this) {
  is SqlRaiseExpr -> IntermediateType(NULL)
  is SqlCaseExpr -> childOfType(SqlTypes.THEN)!!.nextSiblingOfType<SqlExpr>().type()

  is SqlExistsExpr -> {
    val isExists = childOfType(SqlTypes.EXISTS) != null
    if (isExists) {
      IntermediateType(PrimitiveType.BOOLEAN)
    } else {
      compoundSelectStmt.queryExposed().single().columns.single().element.type()
    }
  }

  is SqlInExpr -> IntermediateType(PrimitiveType.BOOLEAN)
  is SqlBetweenExpr -> IntermediateType(PrimitiveType.BOOLEAN)
  is SqlIsExpr -> IntermediateType(PrimitiveType.BOOLEAN)
  is SqlNullExpr -> IntermediateType(PrimitiveType.BOOLEAN)
  is SqlBinaryLikeExpr -> IntermediateType(PrimitiveType.BOOLEAN)
  is SqlCollateExpr -> expr.type()
  is SqlCastExpr -> typeName.type().nullableIf(expr.type().javaType.isNullable)
  is SqlParenExpr -> expr?.type() ?: IntermediateType(NULL)
  is SqlFunctionExpr -> typeResolver.functionType(this) ?: IntermediateType(NULL)

  is SqlBinaryExpr -> {
    if (childOfType(
        TokenSet.create(
            SqlTypes.EQ, SqlTypes.EQ2, SqlTypes.NEQ,
            SqlTypes.NEQ2, SqlTypes.AND, SqlTypes.OR, SqlTypes.GT, SqlTypes.GTE,
            SqlTypes.LT, SqlTypes.LTE
          )
      ) != null
    ) {
      IntermediateType(PrimitiveType.BOOLEAN)
    } else {
      typeResolver.encapsulatingType(
        exprList = getExprList(),
        nullableIfAny = (this is SqlBinaryAddExpr || this is SqlBinaryMultExpr),
        INTEGER, REAL, TEXT, BLOB
      )
    }
  }

  is SqlUnaryExpr -> expr.type()

  is SqlBindExpr -> IntermediateType(ARGUMENT)

  is SqlLiteralExpr -> when {
    (literalValue.stringLiteral != null) -> IntermediateType(TEXT)
    (literalValue.blobLiteral != null) -> IntermediateType(BLOB)
    (literalValue.numericLiteral != null) -> {
      if (literalValue.text.contains('.')) {
        IntermediateType(REAL)
      } else {
        IntermediateType(INTEGER)
      }
    }
    (
      literalValue.childOfType(
        TokenSet.create(
          SqlTypes.CURRENT_TIMESTAMP,
          SqlTypes.CURRENT_TIME, SqlTypes.CURRENT_DATE
        )
      ) != null
      ) -> IntermediateType(TEXT)
    (literalValue.childOfType(SqlTypes.NULL) != null) -> IntermediateType(NULL)
    else -> IntermediateType(BLOB).asNullable()
  }

  is SqlColumnExpr -> columnName.type()

  is SqlOtherExpr -> {
    extensionExpr.type()
  }

  else -> throw IllegalStateException("Unknown expression type $this")
}
