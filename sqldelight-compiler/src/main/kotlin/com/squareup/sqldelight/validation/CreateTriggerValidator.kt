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
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.types.Resolver
import com.squareup.sqldelight.types.Value

internal class CreateTriggerValidator(val resolver: Resolver) {
  fun validate(trigger: SqliteParser.Create_trigger_stmtContext) {
    var tableColumns = resolver.resolve(trigger.table_name())

    trigger.column_name().forEach { column ->
      if (tableColumns.filter({ it.columnName == column.text }).isEmpty()) {
        throw SqlitePluginException(column,
            "Column ${column.text} does not exist in table ${trigger.table_name().text}")
      }
    }

    val availableColumns = availableColumns(trigger, tableColumns)

    if (trigger.expr() != null) {
      ExpressionValidator(resolver, availableColumns).validate(trigger.expr())
    }

    trigger.select_stmt().forEach {
      resolver.resolve(it) // This gets us the columns back and validates.
    }

    trigger.insert_stmt().forEach {
      InsertValidator(resolver, availableColumns).validate(it)
    }

    trigger.delete_stmt().forEach {
      DeleteValidator(resolver, availableColumns).validate(it)
    }

    trigger.update_stmt().forEach {
      UpdateValidator(resolver, availableColumns).validate(it)
    }
  }

  fun availableColumns(
      trigger: SqliteParser.Create_trigger_stmtContext,
      tableValues: List<Value>
  ): List<Value> {
    if (trigger.K_INSERT() != null) {
      return tableValues + tableValues.map { Value("new", it.columnName, it.type, it.element) }
    }
    if (trigger.K_UPDATE() != null) {
      return tableValues + tableValues.flatMap {
        listOf(Value("new", it.columnName, it.type, it.element),
            Value("old", it.columnName, it.type, it.element))
      }
    }
    if (trigger.K_DELETE() != null) {
      return tableValues + tableValues.map { Value("old", it.columnName, it.type, it.element) }
    }

    throw IllegalStateException("Did not know how to handle create trigger statement")
  }
}