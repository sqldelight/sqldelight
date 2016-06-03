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
package com.squareup.sqldelight.validation

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.resolution.Resolution
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.types.Value
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet

data class QueryResults private constructor(
    internal val queryName: String,
    /**
     * Individual columns that map to a single SQLite data type. Each one will have a method in the
     * generated interface.
     */
    internal val columns: Map<String, IndexedValue>,
    /**
     * Full tables that are part of the query results. Each one will have a method in
     * the generated interface, and creating a mapper for this query will require its own
     * RowMapper<TableType>.
     */
    internal val tables: Map<String, QueryTable>
) {
  companion object {
    /**
     * Take as input the resolution of a select statement and the current context's SymbolTable
     * and return a query results object consumable by the compiler.
     */
    internal fun create(
        resolution: Resolution,
        symbolTable: SymbolTable,
        queryName: String
    ): QueryResults {
      val methodNames = LinkedHashSet<String>()

      // First separate out all the columns with tables from the expressions.
      val columnsForTableName = LinkedHashMap<String, MutableSet<IndexedValue>>()
      val columnDefsForTableName = LinkedHashMap<String, MutableSet<SqliteParser.Column_defContext>>()
      val expressions = ArrayList<IndexedValue>()
      resolution.values.forEachIndexed { index, value ->
        if (value.tableName != null && value.element is SqliteParser.Column_defContext) {
          // Value is part of a table.
          val columnDefs = columnDefsForTableName.getOrPut(value.tableName) {
            LinkedHashSet<SqliteParser.Column_defContext>()
          }

          if (!columnDefs.add(value.element)) {
            // This column was already added so we have a duplicate.
            expressions.add(index to value)
          } else {
            // Column not yet added
            val columns = columnsForTableName.getOrPut(value.tableName) {
              LinkedHashSet<IndexedValue>()
            }
            columns.add(index to value)
          }
        } else {
          // Value is an expression or was aliased.
          expressions.add(index to value)
        }
      }

      // Find the complete tables in the resolution.
      val tables = LinkedHashMap<String, QueryTable>()
      symbolTable.tables.values.forEach { createTable ->
        columnsForTableName.forEach { tableName, columns ->
          if (createTable.column_def().size == columns.size &&
              createTable.column_def().containsAll(columns.map { it.value.element })) {
            // Same table, add it as a full table query result.
            methodNames.add(tableName)
            tables.put(tableName, QueryTable(createTable, columns))
          }
        }
      }

      // Take the columns from incomplete tables in the resolution and add them to the expression list.
      expressions.addAll(columnsForTableName
          .filter { !tables.keys.contains(it.key) }
          .flatMap { it.value })

      // Add all the expressions in as individual columns.
      val columns = LinkedHashMap<String, IndexedValue>()
      for((index, value) in expressions) {
        var methodName: String
        if (value.columnName != null) {
          methodName = value.columnName
          if (methodNames.add(methodName)) {
            columns.put(methodName, index to value)
            continue
          }

          if (value.tableName != null) {
            methodName = "${value.tableName}_${value.columnName}"
            if (methodNames.add(methodName)) {
              columns.put(methodName, index to value)
              continue
            }
          }
        } else if (value.element is SqliteParser.ExprContext) {
          methodName = value.element.methodName() ?: "expr"
        } else if (value.element is SqliteParser.Literal_valueContext) {
          methodName = value.element.methodName()
        } else {
          methodName = value.element.text
        }

        var i = 2
        var suffixedMethodName = methodName
        while (!methodNames.add(suffixedMethodName)) {
          suffixedMethodName = "${methodName}_${i++}"
        }
        columns.put(suffixedMethodName, index to value)
      }

      return QueryResults(queryName, columns, tables)
    }

    private fun SqliteParser.ExprContext.methodName(): String? {
      if (column_name() != null) {
        if (table_name() != null) {
          return "${table_name().text}_${column_name().text}"
        }
        return column_name().text
      }
      if (literal_value() != null) {
        return literal_value().methodName()
      }
      if (function_name() != null) {
        if (expr().size == 0) {
          return function_name().text
        }
        return "${function_name().text}_${expr(0).methodName() ?: return function_name().text}"
      }
      return null
    }

    private fun SqliteParser.Literal_valueContext.methodName(): String {
      if (INTEGER_LITERAL() != null) {
        return "int_literal"
      }
      if (REAL_LITERAL() != null) {
        return "real_literal"
      }
      if (STRING_LITERAL() != null) {
        return "string_literal"
      }
      if (BLOB_LITERAL() != null) {
        return "blob_literal"
      }
      return "literal"
    }
  }

  /**
   * Keep track of the index for values so that we can avoid using Cursor.getColumnIndex()
   */
  internal data class IndexedValue(val index: Int, val value: Value)

  /**
   * The compiler expects an ANTLR rule so keep track of both it and the indexed values.
   */
  internal data class QueryTable(
      val table: SqliteParser.Create_table_stmtContext,
      val indexedValues: Set<IndexedValue>
  )
}

private infix fun Int.to(that: Value) = QueryResults.IndexedValue(this, that)
