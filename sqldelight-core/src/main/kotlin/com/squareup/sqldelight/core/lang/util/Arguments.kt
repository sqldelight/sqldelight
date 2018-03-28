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
import com.alecstrong.sqlite.psi.core.psi.SqliteCompoundSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteFunctionExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteInExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteIsExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteLimitingTerm
import com.alecstrong.sqlite.psi.core.psi.SqliteNullExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteParenExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteSetterExpression
import com.alecstrong.sqlite.psi.core.psi.SqliteUnaryExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmtLimited
import com.alecstrong.sqlite.psi.core.psi.SqliteValuesExpression
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.ARGUMENT
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.INTEGER
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.NULL
import com.squareup.sqldelight.core.lang.IntermediateType.SqliteType.TEXT

/**
 * Return the expected type for this expression, which is the argument type exposed in the generated
 * api.
 */
internal fun SqliteBindExpr.argumentType(): IntermediateType {
  val parentRule = parent!!
  val argument = when (parentRule) {
    is SqliteExpr -> parentRule.argumentType(this)
    is SqliteValuesExpression -> parentRule.argumentType(this)
    is SqliteSetterExpression -> parentRule.argumentType(this)
    is SqliteLimitingTerm -> IntermediateType(INTEGER)
    else -> IntermediateType(NULL, Any::class.asClassName())
  }
  return argument.copy(bindArg = this)
}

internal fun SqliteBindExpr.isArrayParameter(): Boolean {
  return (parent is SqliteInExpr && this == parent.lastChild)
}

/**
 * Return the expected type for [argument], which is the argument type exposed in the generated api.
 */
private fun SqliteExpr.argumentType(argument: SqliteExpr): IntermediateType {
  return when (this) {
    is SqliteInExpr -> {
      if (argument === firstChild) return IntermediateType(ARGUMENT)

      return exprList.first().type()
    }

    is SqliteCaseExpr, is SqliteBetweenExpr, is SqliteIsExpr, is SqliteBinaryExpr -> {
      children.last { it is SqliteExpr && it !== argument }.type()
    }

    is SqliteNullExpr -> IntermediateType(NULL).asNullable()
    is SqliteBinaryLikeExpr -> {
      val other = children.last { it is SqliteExpr && it !== argument }.type()
      IntermediateType(TEXT).copy(name = other.name)
    }

    is SqliteCollateExpr, is SqliteCastExpr, is SqliteParenExpr, is SqliteUnaryExpr -> {
      return IntermediateType(ARGUMENT)
    }

    is SqliteFunctionExpr -> {
      return IntermediateType(ARGUMENT)
    }
    else -> throw AssertionError()
  }
}

private fun SqliteValuesExpression.argumentType(expression: SqliteExpr): IntermediateType {
  val argumentIndex = children.indexOf(expression)
  if (argumentIndex == -1) throw AssertionError()

  val parentRule = parent!!
  return when (parentRule) {
    is SqliteInsertStmt -> parentRule.columns[argumentIndex].type()
    is SqliteSelectStmt -> {
      val compoundSelect = parentRule.parent as SqliteCompoundSelectStmt
      NamedQuery("temp", compoundSelect).resultColumns[argumentIndex]
    }

    else -> throw AssertionError()
  }
}

private fun SqliteSetterExpression.argumentType(child: SqliteBindExpr): IntermediateType {
  val parentRule = parent!!
  val column = when (parentRule) {
    is SqliteUpdateStmt -> parentRule.columnNameList[parentRule.setterExpressionList.indexOf(this)]
    is SqliteUpdateStmtLimited -> parentRule.columnNameList[parentRule.setterExpressionList.indexOf(this)]
    else -> throw AssertionError()
  }

  return column.type()
}