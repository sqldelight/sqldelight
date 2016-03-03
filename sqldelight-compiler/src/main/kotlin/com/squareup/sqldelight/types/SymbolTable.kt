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
import java.util.ArrayList
import java.util.LinkedHashMap

class SymbolTable {
  internal val tables: LinkedHashMap<String, SqliteParser.Create_table_stmtContext>
  internal val views: LinkedHashMap<String, SqliteParser.Create_view_stmtContext>

  constructor(parsed: SqliteParser.ParseContext) {
    tables = if (parsed.sql_stmt_list().create_table_stmt() != null) {
      linkedMapOf(parsed.sql_stmt_list().create_table_stmt().table_name().text
          to parsed.sql_stmt_list().create_table_stmt())
    } else {
      linkedMapOf()
    }
    views = linkedMapOf(*parsed.sql_stmt_list().sql_stmt()
        .map { it.create_view_stmt() }
        .filterNotNull()
        .map { it.view_name().text to it }
        .toTypedArray())
  }

  constructor() {
    tables = linkedMapOf()
    views = linkedMapOf()
  }

  private val tableTags = linkedMapOf<Any, ArrayList<String>>()
  private val viewTags = linkedMapOf<Any, ArrayList<String>>()

  private fun remove(tag: Any) {
    for (tableName in tableTags.getOrEmpty(tag)) {
      tables.remove(tableName)
    }
    for (viewName in viewTags.getOrEmpty(tag)) {
      views.remove(viewName)
    }
  }

  fun setSymbolsForTag(other: SymbolTable, tag: Any) {
    // Remove all the existing tables for the given tag.
    remove(tag)

    for ((name, table) in other.tables) {
      if (tables.containsKey(name)) {
        throw SqlitePluginException(table.table_name(), "Table already defined with name $name")
      }
      if (views.containsKey(name)) {
        throw SqlitePluginException(table.table_name(), "View already defined with name $name")
      }
      tables.put(name, table)
      tableTags.put(tag, name)
    }

    for ((name, view) in other.views) {
      if (tables.containsKey(name)) {
        throw SqlitePluginException(view.view_name(), "Table already defined with name $name")
      }
      if (views.containsKey(name)) {
        throw SqlitePluginException(view.view_name(), "View already defined with name $name")
      }
      views.put(name, view)
      viewTags.put(tag, name)
    }
  }

  private fun <K, V> LinkedHashMap<K, ArrayList<V>>.getOrEmpty(key: K) = get(key) ?: arrayListOf()

  private fun <K, V> LinkedHashMap<K, ArrayList<V>>.put(key: K, value: V) {
    var values = get(key)
    if (values == null) {
      values = arrayListOf()
      put(key, values)
    }
    values.add(value)
  }
}