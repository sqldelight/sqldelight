/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqldelight.types

import com.squareup.javapoet.TypeName
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.validation.JoinValidator
import org.antlr.v4.runtime.ParserRuleContext
import java.util.ArrayList

/**
 * The only job of this class is to return an ordered list of values that any given
 * rule evaluates to.
 */
class Resolver(private val symbolTable: SymbolTable) {
  val currentlyResolvingViews = linkedSetOf<String>()

  fun resolve(selectStmt: SqliteParser.Select_stmtContext) = resolve(selectStmt.select_or_values(0))

  fun resolve(selectOrValues: SqliteParser.Select_or_valuesContext): List<Value> {
    val values: List<Value>
    if (selectOrValues.K_VALUES() != null) {
      return selectOrValues.expr().map { resolve(it, emptyList()) }
    } else if (selectOrValues.join_clause() != null) {
      values = resolve(selectOrValues.join_clause())
    } else if (selectOrValues.table_or_subquery().size > 0) {
      values = selectOrValues.table_or_subquery().flatMap { resolve(it) }
    } else {
      throw SqlitePluginException(selectOrValues,
          "Resolver did not know how to handle select or values")
    }

    // Filter down the values by the ones used as result columns
    return selectOrValues.result_column().flatMap { resolve(it, values) }
  }

  fun resolve(
      resultColumn: SqliteParser.Result_columnContext,
      availableValues: List<Value>
  ): List<Value> {
    if (resultColumn.text.equals("*")) {
      return availableValues
    }
    if (resultColumn.table_name() != null) {
      return availableValues.filter { it.tableName == resultColumn.table_name().text }
    }
    if (resultColumn.expr() != null) {
      var value = resolve(resultColumn.expr(), availableValues)
      if (resultColumn.K_AS() != null) {
        value = Value(value.tableName, resultColumn.column_alias().text, value.type, value.element)
      }
      return listOf(value)
    }
    throw SqlitePluginException(resultColumn, "Resolver did not know how to handle result column")
  }

  fun resolve(expression: SqliteParser.ExprContext, availableValues: List<Value>): Value {
    if (expression.column_name() != null) {
      // | ( ( database_name '.' )? table_name '.' )? column_name
      val matchingColumns = availableValues.columns(expression.column_name().text,
          expression.table_name()?.text)
      if (matchingColumns.isEmpty()) {
        throw SqlitePluginException(expression,
            "No column found with name ${expression.column_name().text}")
      }
      if (matchingColumns.size > 1) {
        throw SqlitePluginException(expression,
            "Ambiguous column name ${expression.column_name().text}, " +
                "founds in tables ${matchingColumns.map { it.tableName }}")
      }
      return matchingColumns[0]
    }

    // TODO get the actual type of the expression. Thats gonna be fun. :(
    return Value(null, null, TypeName.VOID, expression)
  }

  fun resolve(joinClause: SqliteParser.Join_clauseContext): List<Value> {
    // Joins are complex because they are in a partial resolution state: They know about
    // values up to the point of this join but not afterward. Because of this, a validation step
    // for joins must happen as part of the resolution step.
    val values = ArrayList(resolve(joinClause.table_or_subquery(0)))
    joinClause.table_or_subquery().drop(1).zip(joinClause.join_constraint(), { table, constraint ->
      val localValues = resolve(table)
      JoinValidator(this, localValues, values).validate(constraint)
      values.addAll(localValues)
    })
    return values
  }

  fun resolve(tableOrSubquery: SqliteParser.Table_or_subqueryContext): List<Value> {
    var originalColumns: List<Value>
    if (tableOrSubquery.table_name() != null) {
      originalColumns = resolve(tableOrSubquery.table_name())
    } else if (tableOrSubquery.select_stmt() != null) {
      originalColumns = resolve(tableOrSubquery.select_stmt())
    } else if (tableOrSubquery.table_or_subquery().size > 0) {
      originalColumns = tableOrSubquery.table_or_subquery().flatMap { resolve(it) }
    } else if (tableOrSubquery.join_clause() != null) {
      originalColumns = resolve(tableOrSubquery.join_clause())
    } else {
      throw SqlitePluginException(tableOrSubquery,
          "Resolver did not know how to handle table or subquery")
    }

    // Alias the values if an alias was given.
    if (tableOrSubquery.table_alias() != null) {
      originalColumns = originalColumns.map {
        Value(tableOrSubquery.table_alias().text, it.columnName, it.type, it.element)
      }
    }

    return originalColumns
  }

  fun resolve(tableName: ParserRuleContext): List<Value> {
    val createTable = symbolTable.tables[tableName.text]
    if (createTable != null) {
      return createTable.column_def().map {
        Value(createTable.table_name().text, it.column_name().text, it.javaType, it)
      }
    } else {
      val view = symbolTable.views[tableName.text] ?: throw SqlitePluginException(tableName,
          "Cannot find table or view ${tableName.text}")
      if (!currentlyResolvingViews.add(view.view_name().text)) {
        throw SqlitePluginException(view.view_name(),
            "Recursive subquery found: ${currentlyResolvingViews.joinToString(" -> ")} -> ${view.view_name().text}")
      }
      val result = resolve(view.select_stmt())
      currentlyResolvingViews.remove(view.view_name().text)
      return result
    }
  }
}