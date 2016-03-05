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

class SymbolTable private constructor(
    internal val tables: Map<String, SqliteParser.Create_table_stmtContext>,
    internal val views: Map<String, SqliteParser.Create_view_stmtContext>,
    internal val commonTables: Map<String, SqliteParser.Common_table_expressionContext>,
    private val tableTags: Map<Any, List<String>>,
    private val viewTags: Map<Any, List<String>>
) {
  constructor(
      parsed: SqliteParser.ParseContext
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
      emptyMap(),
      emptyMap(),
      emptyMap()
  )

  constructor(
      commonTable: SqliteParser.Common_table_expressionContext
  ) : this(
      emptyMap(),
      emptyMap(),
      mapOf(commonTable.table_name().text to commonTable),
      emptyMap(),
      emptyMap()
  )

  constructor() : this(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())

  fun merge(other: SymbolTable, otherTag: Any): SymbolTable {
    val tables = LinkedHashMap(this.tables)
    tableTags.filter({ it.key == otherTag }).flatMap({ it.value }).forEach { tables.remove(it) }
    tables.keys.intersect(other.tables.keys).forEach {
      throw SqlitePluginException(other.tables[it]!!.table_name(),
          "Table already defined with name $it")
    }
    tables.keys.intersect(other.views.keys).forEach {
      throw SqlitePluginException(other.views[it]!!.view_name(),
          "Table already defined with name $it")
    }
    tables.keys.intersect(other.commonTables.keys).forEach {
      throw SqlitePluginException(other.commonTables[it]!!.table_name(),
          "Table already defined with name $it")
    }

    val views = LinkedHashMap(this.views)
    viewTags.filter({ it.key == otherTag }).flatMap({ it.value }).forEach { views.remove(it) }
    views.keys.intersect(other.tables.keys).forEach {
      throw SqlitePluginException(other.tables[it]!!.table_name(),
          "View already defined with name $it")
    }
    views.keys.intersect(other.views.keys).forEach {
      throw SqlitePluginException(other.views[it]!!.view_name(),
          "View already defined with name $it")
    }
    views.keys.intersect(other.commonTables.keys).forEach {
      throw SqlitePluginException(other.commonTables[it]!!.table_name(),
          "View already defined with name $it")
    }

    return SymbolTable(
        tables + other.tables,
        views + other.views,
        this.commonTables + other.commonTables,
        this.tableTags + (otherTag to other.tables.map { it.key }),
        this.viewTags + (otherTag to other.views.map { it.key })
    )
  }
}