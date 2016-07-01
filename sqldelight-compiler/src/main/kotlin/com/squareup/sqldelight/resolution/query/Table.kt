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
package com.squareup.sqldelight.resolution.query

import com.squareup.javapoet.ClassName
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.types.SymbolTable
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Correponds to a single table result in a SQL select. Example:
 *
 *  1. SELECT table.* FROM table;
 *
 */
internal data class Table private constructor(
    override val name: String,
    override val javaType: ClassName,
    override val element: ParserRuleContext,
    override val nullable: Boolean,
    val table: SqliteParser.Create_table_stmtContext
) : Result {
  constructor(table: SqliteParser.Create_table_stmtContext, symbolTable: SymbolTable) : this (
      table.table_name().text,
      symbolTable.tableTypes[table.table_name().text]!!,
      table.table_name(),
      false,
      table
  )

  override fun tableNames() = listOf(name)
  override fun columnNames() = table.column_def().map { it.column_name().text }
  override fun size() = table.column_def().size
  override fun findElement(columnName: String, tableName: String?): List<Value> {
    if (tableName == null || tableName == name) {
      return table.column_def().map {
        val value = Value(it, javaType, name)
        return@map if (nullable) value.copy(nullable = true) else value
      }.filter { it.name == columnName }
    }
    return emptyList()
  }
}
