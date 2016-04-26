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
import com.squareup.sqldelight.types.ResolutionError
import com.squareup.sqldelight.types.Resolver
import com.squareup.sqldelight.types.Value
import java.util.ArrayList

internal class CreateTriggerValidator(val resolver: Resolver) {
  fun validate(trigger: SqliteParser.Create_trigger_stmtContext) : List<ResolutionError> {
    val resolution = resolver.resolve(trigger.table_name())
    val response = ArrayList(resolution.errors)

    trigger.column_name().forEach { column ->
      if (resolution.values.filter({ it.columnName == column.text }).isEmpty()) {
        response.add(ResolutionError.ColumnNameNotFound(
            column,
            "Column ${column.text} does not exist in table ${trigger.table_name().text}",
            resolution.values
        ))
      }
    }

    val availableColumns = availableColumns(trigger, resolution.values)

    if (trigger.expr() != null) {
      response.addAll(ExpressionValidator(resolver, availableColumns).validate(trigger.expr()))
    }

    response.addAll(trigger.select_stmt().flatMap {
      resolver.resolve(it).errors // This gets us the columns back and validates.
    })

    response.addAll(trigger.insert_stmt().flatMap {
      InsertValidator(resolver, availableColumns).validate(it)
    })

    response.addAll(trigger.delete_stmt().flatMap {
      DeleteValidator(resolver, availableColumns).validate(it)
    })

    response.addAll(trigger.update_stmt().flatMap {
      UpdateValidator(resolver, availableColumns).validate(it)
    })

    return response
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