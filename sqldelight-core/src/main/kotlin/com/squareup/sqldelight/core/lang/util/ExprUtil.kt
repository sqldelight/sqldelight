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
import com.alecstrong.sqlite.psi.core.psi.SqliteLikeExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteLiteralExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteNullExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteParenExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteRaiseExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.alecstrong.sqlite.psi.core.psi.SqliteUnaryExpr
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.lang.psi.SqliteTypeMixin

internal val SqliteExpr.name: String get() = when(this) {
  is SqliteCastExpr -> expr.name
  is SqliteParenExpr -> expr.name
  is SqliteFunctionExpr -> functionName.text
  is SqliteColumnExpr -> columnName.name
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
internal fun SqliteExpr.type(javaType: Boolean = false): TypeName = when(this) {
  is SqliteRaiseExpr -> UNIT
  is SqliteCaseExpr -> PsiTreeUtil.getNextSiblingOfType(node.findChildByType(SqliteTypes.THEN)!!.psi,
        SqliteExpr::class.java)!!.type(javaType)
  is SqliteExistsExpr -> if (javaType) BOOLEAN else INT
  is SqliteInExpr -> if (javaType) BOOLEAN else INT
  is SqliteBetweenExpr -> if (javaType) BOOLEAN else INT
  is SqliteIsExpr -> if (javaType) BOOLEAN else INT
  is SqliteNullExpr -> if (javaType) BOOLEAN else INT
  is SqliteLikeExpr -> if (javaType) BOOLEAN else INT
  is SqliteCollateExpr -> expr.type()
  is SqliteCastExpr -> (typeName as SqliteTypeMixin).type()
  is SqliteParenExpr -> expr.type()
  is SqliteFunctionExpr -> functionType()

  // TODO (AlecStrong) These are not actually true. AND returns BOOLEAN for example
  is SqliteBinaryExpr -> exprList[0].type()
  is SqliteUnaryExpr -> expr.type()

  is SqliteBindExpr -> TODO("Get type of bind expression")

  is SqliteLiteralExpr -> when {
    (literalValue.stringLiteral != null) -> STRING
    (literalValue.blobLiteral != null) -> BLOB
    (literalValue.numericLiteral != null) -> FLOAT
    (literalValue.node.findChildByType(TokenSet.create(SqliteTypes.CURRENT_TIMESTAMP,
        SqliteTypes.CURRENT_TIME, SqliteTypes.CURRENT_DATE)) != null) -> STRING
    else -> BLOB.asNullable()
  }

  is SqliteColumnExpr -> columnName.reference!!.resolve()!!.type(javaType)
  else -> throw AssertionError()
}

private fun SqliteFunctionExpr.functionType(): TypeName = result@when (functionName.text) {

  "round" -> {
    // Single arg round function returns an int. Otherwise real.
    if (exprList.size == 1) {
      return@result INT.nullableIf(exprList[0].type().nullable)
    }
    return@result FLOAT.nullableIf(exprList.any { it.type().nullable })
  }

  "sum" -> {
    val type = exprList[0].type()
    if (type.asNonNullable() == INT) {
      return@result type
    }
    return@result FLOAT.nullableIf(type.nullable)
  }

  "lower", "ltrim", "printf", "replace", "rtrim", "substr", "trim", "upper", "group_concat" -> {
    STRING.nullableIf(exprList[0].type().nullable)
  }

  "date", "time", "datetime", "julianday", "strftime", "char", "hex", "quote", "soundex",
  "sqlite_compileoption_get", "sqlite_source_id", "sqlite_version", "typeof" -> {
    STRING
  }

  "changes", "last_insert_rowid", "random", "sqlite_compileoption_used" -> INT
  "total_changes", "count" -> INT
  "instr", "length", "unicode" -> INT.nullableIf(exprList.any { it.type().nullable })
  "randomblob", "zeroblob" -> BLOB
  "total", "avg" -> FLOAT
  "abs", "likelihood", "likely", "unlikely" -> exprList[0].type()
  "coalesce", "ifnull" -> encapsulatingType(exprList, INT, FLOAT, STRING, BLOB)
  "nullif" -> exprList[0].type().asNullable()
  "max" -> encapsulatingType(exprList, INT, FLOAT, STRING, BLOB)
  "min" -> encapsulatingType(exprList, BLOB, STRING, INT, FLOAT)
  else -> throw AssertionError()
}

private fun TypeName.nullableIf(predicate: Boolean): TypeName {
  return if (predicate) asNullable() else asNonNullable()
}

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
private fun encapsulatingType(exprList: List<SqliteExpr>, vararg typeOrder: TypeName): TypeName {
  val types = exprList.map { it.type() }
  val nonNullTypes = types.map { it.asNonNullable() }

  val type = typeOrder.last { it in nonNullTypes }
  if (types.any { it.nullable }) return type.asNullable()
  return type
}

private val STRING = String::class.asClassName()
private val BLOB = ByteArray::class.asClassName()