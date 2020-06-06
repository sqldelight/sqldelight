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

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.psi.SqlBetweenExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryLikeExpr
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
import com.alecstrong.sql.psi.core.psi.SqlParenExpr
import com.alecstrong.sql.psi.core.psi.SqlRaiseExpr
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlUnaryExpr
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
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.type

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
  is SqlCastExpr -> typeName.type()
  is SqlParenExpr -> expr?.type() ?: IntermediateType(NULL)
  is SqlFunctionExpr -> functionType()

  is SqlBinaryExpr -> {
    if (childOfType(TokenSet.create(SqlTypes.EQ, SqlTypes.EQ2, SqlTypes.NEQ,
        SqlTypes.NEQ2, SqlTypes.AND, SqlTypes.OR, SqlTypes.GT, SqlTypes.GTE,
        SqlTypes.LT, SqlTypes.LTE)) != null) {
      IntermediateType(INTEGER, BOOLEAN)
    } else {
      encapsulatingType(getExprList(), INTEGER, REAL, TEXT, BLOB)
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
    (literalValue.childOfType(TokenSet.create(SqlTypes.CURRENT_TIMESTAMP,
        SqlTypes.CURRENT_TIME, SqlTypes.CURRENT_DATE)) != null) -> IntermediateType(TEXT)
    (literalValue.childOfType(SqlTypes.NULL) != null) -> IntermediateType(NULL)
    else -> IntermediateType(BLOB).asNullable()
  }

  is SqlColumnExpr -> columnName.type()
  else -> throw AssertionError()
}

private fun SqlFunctionExpr.functionType() = when (functionName.text.toLowerCase()) {

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
    if (type.sqliteType == INTEGER && !type.javaType.isNullable) {
      type.asNullable()
    } else {
      IntermediateType(REAL).asNullable()
    }
  }

  "lower", "ltrim", "printf", "replace", "rtrim", "substr", "trim", "upper", "group_concat" -> {
    IntermediateType(TEXT).nullableIf(exprList[0].type().javaType.isNullable)
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
    IntermediateType(INTEGER).nullableIf(exprList.any { it.type().javaType.isNullable })
  }

  "randomblob", "zeroblob" -> IntermediateType(BLOB)
  "total" -> IntermediateType(REAL)
  "avg" -> IntermediateType(REAL).asNullable()
  "abs", "likelihood", "likely", "unlikely" -> exprList[0].type()
  "coalesce", "ifnull" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
  "nullif" -> exprList[0].type().asNullable()
  "max" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB).asNullable()
  "min" -> encapsulatingType(exprList, BLOB, TEXT, INTEGER, REAL).asNullable()
  else -> when ((containingFile as SqlDelightFile).dialect) {
    DialectPreset.MYSQL -> mySqlFunctionType()
    DialectPreset.POSTGRESQL -> postgreSqlFunctionType()
    else -> throw AssertionError("Unknown function")
  }
}

private fun SqlFunctionExpr.mySqlFunctionType() = when (functionName.text.toLowerCase()) {
  "greatest" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
  "concat" -> encapsulatingType(exprList, TEXT)
  "month" -> IntermediateType(INTEGER)
  "year" -> IntermediateType(INTEGER)
  else -> throw AssertionError("Unknown function for MySQL: ${functionName.text}")
}

private fun SqlFunctionExpr.postgreSqlFunctionType() = when (functionName.text.toLowerCase()) {
  "greatest" -> encapsulatingType(exprList, INTEGER, REAL, TEXT, BLOB)
  "concat" -> encapsulatingType(exprList, TEXT)
  else -> throw AssertionError("Unknown function for PostgreSQL: ${functionName.text}")
}

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
private fun encapsulatingType(
  exprList: List<SqlExpr>,
  vararg typeOrder: SqliteType
): IntermediateType {
  val types = exprList.map { it.type() }
  val sqlTypes = types.map { it.sqliteType }

  val type = typeOrder.last { it in sqlTypes }
  if (types.all { it.javaType.isNullable }) {
    return IntermediateType(type).asNullable()
  }
  return IntermediateType(type)
}
