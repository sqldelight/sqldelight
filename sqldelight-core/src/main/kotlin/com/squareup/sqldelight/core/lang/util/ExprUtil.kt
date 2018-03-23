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
package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.SqliteBetweenExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteBinaryExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteBinaryLikeExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteBindExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCaseExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCastExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCollateExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteColumnExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteExistsExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteFunctionExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteInExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteIsExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteLiteralExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteNullExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteParenExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteRaiseExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.alecstrong.sqlite.psi.core.psi.SqliteUnaryExpr
import com.intellij.psi.tree.TokenSet
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.ARGUMENT
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.BLOB
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.INTEGER
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.NULL
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.REAL
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT
import com.squareup.sqldelight.core.lang.psi.SqliteTypeMixin

internal val SqliteExpr.name: String get() = when(this) {
  is SqliteCastExpr -> expr.name
  is SqliteParenExpr -> expr?.name ?: "value"
  is SqliteFunctionExpr -> functionName.text
  is SqliteColumnExpr -> allocateName(columnName)
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
internal fun SqliteExpr.type(): IntermediateType = when(this) {
  is SqliteRaiseExpr -> IntermediateType(NULL)
  is SqliteCaseExpr -> childOfType(SqliteTypes.THEN)!!.nextSiblingOfType<SqliteExpr>().type()
  is SqliteExistsExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqliteInExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqliteBetweenExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqliteIsExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqliteNullExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqliteBinaryLikeExpr -> IntermediateType(INTEGER, BOOLEAN)
  is SqliteCollateExpr -> expr.type()
  is SqliteCastExpr -> (typeName as SqliteTypeMixin).type()
  is SqliteParenExpr -> expr?.type() ?: IntermediateType(NULL)
  is SqliteFunctionExpr -> functionType()

  is SqliteBinaryExpr -> {
    if (childOfType(TokenSet.create(SqliteTypes.EQ, SqliteTypes.EQ2, SqliteTypes.NEQ,
        SqliteTypes.NEQ2, SqliteTypes.AND, SqliteTypes.OR, SqliteTypes.GT, SqliteTypes.GTE,
        SqliteTypes.LT, SqliteTypes.LTE)) != null) {
      IntermediateType(INTEGER, BOOLEAN)
    } else {
      getExprList()[0].type()
    }
  }

  is SqliteUnaryExpr -> expr.type()

  is SqliteBindExpr -> IntermediateType(ARGUMENT)

  is SqliteLiteralExpr -> when {
    (literalValue.stringLiteral != null) -> IntermediateType(TEXT)
    (literalValue.blobLiteral != null) -> IntermediateType(BLOB)
    (literalValue.numericLiteral != null) -> IntermediateType(REAL)
    (literalValue.childOfType(TokenSet.create(SqliteTypes.CURRENT_TIMESTAMP,
        SqliteTypes.CURRENT_TIME, SqliteTypes.CURRENT_DATE)) != null) -> IntermediateType(TEXT)
    (literalValue.childOfType(SqliteTypes.NULL) != null) -> IntermediateType(NULL)
    else -> IntermediateType(BLOB).asNullable()
  }

  is SqliteColumnExpr -> columnName.type()
  else -> throw AssertionError()
}

private fun SqliteFunctionExpr.functionType() = result@when (functionName.text.toLowerCase()) {

  "round" -> {
    // Single arg round function returns an int. Otherwise real.
    if (exprList.size == 1) {
      return@result IntermediateType(INTEGER).nullableIf(exprList[0].type().javaType.nullable)
    }
    return@result IntermediateType(REAL).nullableIf(exprList.any { it.type().javaType.nullable })
  }

  "sum" -> {
    val type = exprList[0].type()
    if (type.sqliteType == INTEGER) {
      return@result type
    }
    return@result IntermediateType(REAL).nullableIf(type.javaType.nullable)
  }

  "lower", "ltrim", "printf", "replace", "rtrim", "substr", "trim", "upper", "group_concat" -> {
    IntermediateType(TEXT).nullableIf(exprList[0].type().javaType.nullable)
  }

  "date", "time", "datetime", "julianday", "strftime", "char", "hex", "quote", "soundex",
  "sqlite_compileoption_get", "sqlite_source_id", "sqlite_version", "typeof" -> {
    IntermediateType(TEXT)
  }

  "changes", "last_insert_rowid", "random", "sqlite_compileoption_used",
  "total_changes", "count" -> {
    IntermediateType(INTEGER)
  }

  "instr", "length", "unicode" -> {
    IntermediateType(INTEGER).nullableIf(exprList.any { it.type().javaType.nullable })
  }

  "randomblob", "zeroblob" -> IntermediateType(BLOB)
  "total", "avg" -> IntermediateType(REAL)
  "abs", "likelihood", "likely", "unlikely" -> exprList[0].type()
  "coalesce", "ifnull" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
  "nullif" -> exprList[0].type().asNullable()
  "max" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
  "min" -> encapsulatingType(exprList, BLOB, TEXT, INTEGER, REAL)
  else -> throw AssertionError()
}

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
private fun encapsulatingType(
    exprList: List<SqliteExpr>,
    vararg typeOrder: SqliteType
): IntermediateType {
  val types = exprList.map { it.type().sqliteType }

  val type = typeOrder.last { it in types }
  if (types.any { it.javaType.nullable }) return IntermediateType(type).asNullable()
  return IntermediateType(type)
}