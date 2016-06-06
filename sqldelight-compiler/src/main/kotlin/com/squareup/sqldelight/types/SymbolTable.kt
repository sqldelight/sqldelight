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

import com.squareup.javapoet.ClassName
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.model.pathAsType
import com.squareup.sqldelight.util.BiMultiMap
import com.squareup.sqldelight.util.emptyBiMultiMap
import com.squareup.sqldelight.util.hasTokenIn
import org.antlr.v4.runtime.Token
import java.util.LinkedHashMap

class SymbolTable constructor(
    internal val tables: Map<String, SqliteParser.Create_table_stmtContext> = emptyMap(),
    internal val views: Map<String, SqliteParser.Create_view_stmtContext> = emptyMap(),
    internal val commonTables: Map<String, SqliteParser.Common_table_expressionContext> = emptyMap(),
    internal val indexes: Map<String, SqliteParser.Create_index_stmtContext> = emptyMap(),
    internal val triggers: Map<String, SqliteParser.Create_trigger_stmtContext> = emptyMap(),
    internal val tableTags: BiMultiMap<Any, String> = emptyBiMultiMap(),
    internal val viewTags: BiMultiMap<Any, String> = emptyBiMultiMap(),
    internal val indexTags: BiMultiMap<Any, String> = emptyBiMultiMap(),
    internal val triggerTags: BiMultiMap<Any, String> = emptyBiMultiMap(),
    internal val tableTypes: Map<String, ClassName> = emptyMap(),
    private val tag: Any? = null
) {
  constructor(
      parsed: SqliteParser.ParseContext,
      tag: Any,
      relativePath: String,
      errors: List<Token> = emptyList()
  ) : this(
      listOf(parsed.sql_stmt_list().create_table_stmt())
        .filterNotNull()
        .filter { it.exception == null && !errors.hasTokenIn(it) }
        .map { it.table_name().text to it }
        .toMap(),
      parsed.sql_stmt_list().sql_stmt()
          .map { it.create_view_stmt() }
          .filterNotNull()
          .filter { it.exception == null && !errors.hasTokenIn(it) }
          .groupBy { it.view_name().text }
          .map {
            val (viewName, views) = it
            if (views.size > 1) {
              throw SqlitePluginException(views[1].view_name(), "Duplicate view name $viewName")
            }
            viewName to views[0]
          }
          .toMap(),
      indexes = parsed.sql_stmt_list().sql_stmt()
          .map { it.create_index_stmt() }
          .filterNotNull()
          .filter { it.exception == null && !errors.hasTokenIn(it) }
          .groupBy { it.index_name().text }
          .map {
            val (indexName, indexes) = it
            if (indexes.size > 1) {
              throw SqlitePluginException(indexes[1].index_name(), "Duplicate index name $indexName")
            }
            indexName to indexes[0]
          }
          .toMap(),
      triggers = parsed.sql_stmt_list().sql_stmt()
          .map { it.create_trigger_stmt() }
          .filterNotNull()
          .filter { it.exception == null && !errors.hasTokenIn(it) }
          .groupBy { it.trigger_name().text }
          .map {
            val (triggerName, triggers) = it
            if (triggers.size > 1) {
              throw SqlitePluginException(triggers[1].trigger_name(), "Duplicate trigger name $triggerName")
            }
            triggerName to triggers[0]
          }
          .toMap(),
      tableTypes = listOf(parsed.sql_stmt_list().create_table_stmt())
          .filterNotNull()
          .filter { it.exception == null && !errors.hasTokenIn(it) }
          .map { it.table_name().text to relativePath.pathAsType() }
          .toMap(),
      tag = tag
  )

  internal constructor(
      commonTable: SqliteParser.Common_table_expressionContext,
      tag: Any
  ) : this(
      commonTables = mapOf(commonTable.table_name().text to commonTable),
      tag = tag
  )

  operator fun minus(tag: Any): SymbolTable {
    return SymbolTable(
        tables.filter { !(tableTags[tag]?.contains(it.key) ?: false) },
        views.filter { !(viewTags[tag]?.contains(it.key) ?: false) },
        commonTables,
        indexes.filter { !(indexTags[tag]?.contains(it.key) ?: false) },
        triggers.filter { !(triggerTags[tag]?.contains(it.key) ?: false) },
        tableTags - tag,
        viewTags - tag,
        indexTags - tag,
        triggerTags - tag,
        tableTypes.filter { !(tableTags[tag]?.contains(it.key) ?: false) }
    )
  }

  operator fun plus(other: SymbolTable): SymbolTable {
    if (other.tag == null) throw IllegalStateException("Symbol tables being added must have a tag")
    val tables = LinkedHashMap(this.tables)
    tableTags.filter({ it.key == other.tag }).flatMap({ it.value }).forEach { tables.remove(it) }
    checkKeys(tables.keys, other, "Table")

    val views = LinkedHashMap(this.views)
    viewTags.filter({ it.key == other.tag }).flatMap({ it.value }).forEach { views.remove(it) }
    checkKeys(views.keys, other, "View")

    checkKeys(commonTables.keys, other, "Common Table")

    val indexes = LinkedHashMap(this.indexes)
    indexTags.filter { it.key == other.tag }.flatMap { it.value }.forEach { indexes.remove(it) }
    indexes.keys.intersect(other.indexes.keys).forEach {
      throw SqlitePluginException(other.indexes[it]!!.index_name(),
          "Index already defined with name $it")
    }

    val triggers = LinkedHashMap(this.triggers)
    triggerTags.filter { it.key == other.tag }.flatMap { it.value }.forEach { triggers.remove(it) }
    triggers.keys.intersect(other.triggers.keys).forEach {
      throw SqlitePluginException(other.triggers[it]!!.trigger_name(),
          "Trigger already defined with name $it")
    }

    return SymbolTable(
        tables + other.tables,
        views + other.views,
        this.commonTables + other.commonTables,
        indexes + other.indexes,
        triggers + other.triggers,
        this.tableTags + BiMultiMap(other.tag to other.tables.map { it.key }),
        this.viewTags + BiMultiMap(other.tag to other.views.map { it.key }),
        this.indexTags + BiMultiMap(other.tag to other.indexes.map { it.key }),
        this.triggerTags + BiMultiMap(other.tag to other.triggers.map { it.key }),
        this.tableTypes + other.tableTypes
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
  }

  override fun toString(): String {
    var result = "\n";
    tableTags.forEach({
      result += "${it.key} -> ${it.value}(\n\t${it.value.map { tables[it]?.text }.joinToString(",\n\t")}\n)\n"
    })
    viewTags.forEach({
      result += "${it.key} -> ${it.value}(\n\t${it.value.map { views[it]?.text }.joinToString(",\n\t")}\n)\n"
    })
    return result
  }
}
