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
import app.cash.sqldelight.core.dialect.mysql.type
import app.cash.sqldelight.core.dialect.sqlite.SqliteType
import app.cash.sqldelight.core.dialect.sqlite.SqliteType.ARGUMENT
import app.cash.sqldelight.core.dialect.sqlite.SqliteType.BLOB
import app.cash.sqldelight.core.dialect.sqlite.SqliteType.INTEGER
import app.cash.sqldelight.core.dialect.sqlite.SqliteType.NULL
import app.cash.sqldelight.core.dialect.sqlite.SqliteType.REAL
import app.cash.sqldelight.core.dialect.sqlite.SqliteType.TEXT
import app.cash.sqldelight.core.lang.psi.FunctionExprMixin
import app.cash.sqldelight.core.lang.psi.type
import com.alecstrong.sql.psi.core.mysql.psi.MySqlExtensionExpr
import app.cash.sqldelight.dialect.api.IntermediateType
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
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlUnaryExpr
import com.intellij.psi.tree.TokenSet
import com.squareup.kotlinpoet.BOOLEAN

internal val SqlExpr.name: String get() = when (this) {
  is SqlCastExpr -> expr.name
  is SqlParenExpr -> expr?.name ?: "value"
  is SqlFunctionExpr -> functionName.text
  is SqlColumnExpr -> allocateName(columnName)
  else -> "expr"
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
internal fun SqlExpr.type(): IntermediateType = when (this) {
  is SqlRaiseExpr -> IntermediateType(NULL)
  is SqlCaseExpr -> childOfType(SqlTypes.THEN)!!.nextSiblingOfType<SqlExpr>().type()

  is SqlExistsExpr -> {
    val isExists = childOfType(SqlTypes.EXISTS) != null
    if (isExists) {
      IntermediateType(INTEGER, BOOLEAN)
    } else {
      compoundSelectStmt.queryExposed().single().columns.single().element.type()
    }
  }

  is SqlInExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqlBetweenExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqlIsExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqlNullExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqlBinaryLikeExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqlCollateExpr -> expr.type()
  is SqlCastExpr -> typeName.type().nullableIf(expr.type().javaType.isNullable)
  is SqlParenExpr -> expr?.type() ?: IntermediateType(NULL)
  is FunctionExprMixin -> functionType() ?: IntermediateType(NULL)

  is SqlBinaryExpr -> {
    if (childOfType(
        TokenSet.create(
            SqlTypes.EQ, SqlTypes.EQ2, SqlTypes.NEQ,
            SqlTypes.NEQ2, SqlTypes.AND, SqlTypes.OR, SqlTypes.GT, SqlTypes.GTE,
            SqlTypes.LT, SqlTypes.LTE
          )
      ) != null
    ) {
      IntermediateType(INTEGER, BOOLEAN)
    } else {
      encapsulatingType(
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

  is MySqlExtensionExpr -> type()
  else -> throw IllegalStateException("Unknown expression type $this")
}

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
internal fun encapsulatingType(
  exprList: List<SqlExpr>,
  vararg typeOrder: SqliteType
) = encapsulatingType(exprList = exprList, nullableIfAny = false, typeOrder = typeOrder)

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
internal fun encapsulatingType(
  exprList: List<SqlExpr>,
  nullableIfAny: Boolean,
  vararg typeOrder: SqliteType
): IntermediateType {
  val types = exprList.map { it.type() }
  val sqlTypes = types.map { it.dialectType }

  val type = typeOrder.last { it in sqlTypes }
  if (!nullableIfAny && types.all { it.javaType.isNullable } ||
    nullableIfAny && types.any { it.javaType.isNullable }
  ) {
    return IntermediateType(type).asNullable()
  }
  return IntermediateType(type)
}
