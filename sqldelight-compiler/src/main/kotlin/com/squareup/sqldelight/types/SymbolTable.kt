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
import java.util.LinkedHashMap

class SymbolTable constructor(
    internal val tables: Map<String, SqliteParser.Create_table_stmtContext> = emptyMap(),
    internal val views: Map<String, SqliteParser.Create_view_stmtContext> = emptyMap(),
    internal val commonTables: Map<String, SqliteParser.Common_table_expressionContext> = emptyMap(),
    internal val withClauses: Map<String, Pair<SqliteParser.Cte_table_nameContext, SqliteParser.Select_stmtContext>> = emptyMap(),
    private val tableTags: Map<Any, List<String>> = emptyMap(),
    private val viewTags: Map<Any, List<String>> = emptyMap(),
    private val tag: Any? = null
) {
  constructor(
      parsed: SqliteParser.ParseContext,
      tag: Any
  ) : this(
      if (parsed.sql_stmt_list().create_table_stmt() != null) {
        linkedMapOf(parsed.sql_stmt_list().create_table_stmt().table_name().text
            to parsed.sql_stmt_list().create_table_stmt())
      } else {
        linkedMapOf()
      },
      linkedMapOf(*parsed.sql_stmt_list().sql_stmt()
          .map { it.create_view_stmt() }
          .filterNotNull()
          .map { it.view_name().text to it }
          .toTypedArray()),
      tag = tag
  )

  internal constructor(
      commonTable: SqliteParser.Common_table_expressionContext,
      tag: Any
  ) : this(
      commonTables = mapOf(commonTable.table_name().text to commonTable),
      tag = tag
  )

  internal constructor(
      withClauses: Pair<SqliteParser.Cte_table_nameContext, SqliteParser.Select_stmtContext>,
      tag: Any
  ) : this (
      withClauses = mapOf(withClauses.first.text to withClauses),
      tag = tag
  )

  operator fun plus(other: SymbolTable): SymbolTable {
    if (other.tag == null) throw IllegalStateException("Symbol tables being added must have a tag")
    val tables = LinkedHashMap(this.tables)
    tableTags.filter({ it.key == other.tag }).flatMap({ it.value }).forEach { tables.remove(it) }
    checkKeys(tables.keys, other, "Table")

    val views = LinkedHashMap(this.views)
    viewTags.filter({ it.key == other.tag }).flatMap({ it.value }).forEach { views.remove(it) }
    checkKeys(views.keys, other, "View")

    checkKeys(commonTables.keys, other, "Common Table")
    checkKeys(withClauses.keys, other, "Common Table")

    return SymbolTable(
        tables + other.tables,
        views + other.views,
        this.commonTables + other.commonTables,
        this.withClauses + other.withClauses,
        this.tableTags + (other.tag to other.tables.map { it.key }),
        this.viewTags + (other.tag to other.views.map { it.key })
    )
  }

  private fun checkKeys(keys: Set<String>, other: SymbolTable, existingText: String) {
    keys.intersect(other.tables.keys).forEach {
      throw SqlitePluginException(other.tables[it]!!.table_name(),
          "$existingText already defined with name $it")
    }
    keys.intersect(other.views.keys).forEach {
      throw SqlitePluginException(other.views[it]!!.view_name(),
          "$existingText already defined with name $it")
    }
    keys.intersect(other.commonTables.keys).forEach {
      throw SqlitePluginException(other.commonTables[it]!!.table_name(),
          "$existingText already defined with name $it")
    }
    keys.intersect(other.withClauses.keys).forEach {
      throw SqlitePluginException(other.withClauses[it]!!.first,
          "$existingText already defined with name $it")
    }
  }
}