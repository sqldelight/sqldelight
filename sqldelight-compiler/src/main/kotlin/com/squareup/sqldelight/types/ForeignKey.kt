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

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.Value
import java.util.ArrayList

internal class ForeignKey private constructor(
    val primaryKey: List<Value>,
    val uniqueConstraints: List<List<Value>>
) {
  companion object {
    /**
     *  Creates a foreign keys object for the given table.
     */
    fun findForeignKeys(symbolTable: SymbolTable, table: Table) =
        ForeignKey(primaryKeys(table), uniqueConstraints(table, symbolTable.indexes.values))

    /**
     * Returns all unique indexes that do not collate which are on the given table.
     */
    private fun uniqueConstraints(
        table: Table,
        indexes: Collection<SqliteParser.Create_index_stmtContext>
    ): List<List<Value>> {
      val result = ArrayList<List<Value>>()

      table.table.column_def().forEach { column ->
        if (column.column_constraint().filter { it.K_UNIQUE() != null }.isNotEmpty()) {
          result.add(listOf(Value(column, table.javaType)))
        }
      }

      table.table.table_constraint().filter { it.K_UNIQUE() != null }.forEach {
        result.add(columnNames(table, it.indexed_column().map { it.column_name() }))
      }

      indexes.filter {
        it.table_name().text == table.name
            && it.indexed_column().all { it.K_COLLATE() == null }
            && it.K_UNIQUE() != null
      }.forEach {
        result.add(columnNames(table, it.indexed_column().map { it.column_name() }))
      }

      return result;
    }

    /**
     * Returns the list of values which represent the primary key on this table.
     */
    private fun primaryKeys(table: Table): List<Value> {
      val primaryKeys = table.table.column_def().filter { column ->
        column.column_constraint().filter { it.K_PRIMARY_KEY() != null }.isNotEmpty()
      }
      if (primaryKeys.size > 1) {
        throw SqlitePluginException(primaryKeys[1], "Can only have one primary key on a table")
      }
      if (primaryKeys.isNotEmpty()) {
        if (table.table.table_constraint().any { it.K_PRIMARY_KEY() != null }) {
          throw SqlitePluginException(table.element, "Can only have one primary key on a table")
        }
        return listOf(Value(primaryKeys[0], table.javaType))
      }

      val tablePrimaryKeys = table.table.table_constraint().filter { it.K_PRIMARY_KEY() != null }
      if (tablePrimaryKeys.size > 1) {
        throw SqlitePluginException(tablePrimaryKeys[1], "Can only have one primary key on a table")
      }
      if (tablePrimaryKeys.size == 0) {
        return emptyList()
      }
      return columnNames(table, tablePrimaryKeys[0].indexed_column().map { it.column_name() })
    }

    /**
     * Turns a list of Column_nameContext into a List of values grabbed from the given table
     * and table columns.
     *
     * @throws SqlitePluginException if a column couldn't be found on the table.
     */
    private fun columnNames(table: Table, columns: List<SqliteParser.Column_nameContext>) =
        columns.map { column ->
          val matches = table.findElement(column.text, table.name)
          if (matches.size == 0) {
            throw SqlitePluginException(column, "No column found with name ${column.text} on table " +
                "${table.name}")
          }
          matches.filterIsInstance<Value>().single()
        }
  }
}
