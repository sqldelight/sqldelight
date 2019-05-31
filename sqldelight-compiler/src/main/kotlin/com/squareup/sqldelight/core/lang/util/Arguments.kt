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
import com.alecstrong.sqlite.psi.core.psi.SqliteExistsExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteFunctionExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteInExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteIsExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteLimitingTerm
import com.alecstrong.sqlite.psi.core.psi.SqliteNullExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteParenExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteResultColumn
import com.alecstrong.sqlite.psi.core.psi.SqliteSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteSetterExpression
import com.alecstrong.sqlite.psi.core.psi.SqliteUnaryExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmtLimited
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmtSubsequentSetter
import com.alecstrong.sqlite.psi.core.psi.SqliteValuesExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
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
  return inferredType().copy(bindArg = this)
}

internal fun SqliteBindExpr.isArrayParameter(): Boolean {
  return (parent is SqliteInExpr && this == parent.lastChild)
}

private fun SqliteExpr.inferredType(): IntermediateType {
  val parentRule = parent!!
  return when (parentRule) {
    is SqliteExpr -> {
      val result = parentRule.argumentType(this)
      if (result.sqliteType == ARGUMENT) {
        parentRule.inferredType()
      } else {
        result
      }
    }

    is SqliteValuesExpression -> parentRule.argumentType(this)
    is SqliteSetterExpression -> parentRule.argumentType()
    is SqliteLimitingTerm -> IntermediateType(INTEGER)
    is SqliteResultColumn -> {
      (parentRule.parent as SqliteSelectStmt).argumentType(parentRule)
          ?: IntermediateType(NULL, Any::class.asClassName())
    }
    else -> IntermediateType(NULL, Any::class.asClassName())
  }
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
      return type()
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
      NamedQuery("temp", "temp", compoundSelect).resultColumns[argumentIndex]
    }

    else -> throw AssertionError()
  }
}

private fun SqliteSelectStmt.argumentType(result: SqliteResultColumn): IntermediateType? {
  val index = resultColumnList.indexOf(result)
  val compoundSelect = parent!! as SqliteCompoundSelectStmt

  val parentRule = compoundSelect.parent ?: return null
  return when (parentRule) {
    is SqliteInsertStmt -> parentRule.columns[index].type()

    else -> {
      // Check if this is part of an inner expression of a resulit column.
      val parentResult = PsiTreeUtil.getParentOfType(parentRule, SqliteResultColumn::class.java)
          ?: return null
      (parentResult.parent as SqliteSelectStmt).argumentType(parentResult)
    }
  }
}


private fun SqliteSetterExpression.argumentType(): IntermediateType {
  val parentRule = parent!!
  val column = when (parentRule) {
    is SqliteUpdateStmt -> parentRule.columnName
    is SqliteUpdateStmtLimited -> parentRule.columnName
    is SqliteUpdateStmtSubsequentSetter -> parentRule.columnName
    else -> throw AssertionError()
  }

  return column!!.type()
}