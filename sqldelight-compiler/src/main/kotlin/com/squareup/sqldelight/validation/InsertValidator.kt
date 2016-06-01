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
import com.squareup.sqldelight.resolution.Resolution
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.resolve
import com.squareup.sqldelight.resolution.ResolutionError
import com.squareup.sqldelight.types.Value
import java.util.ArrayList

internal class InsertValidator(
    var resolver: Resolver,
    val scopedValues: List<Value> = emptyList()
) {
  fun validate(insert: SqliteParser.Insert_stmtContext) : List<ResolutionError> {
    val resolution = resolver.resolve(insert.table_name())
    val response = ArrayList(resolution.errors)
    val columnsForTable = resolution.values.map { it.columnName }

    response.addAll(insert.column_name().filter({ !columnsForTable.contains(it.text) }).map {
      ResolutionError.ColumnNameNotFound(
          it,
          "Column ${it.text} does not exist in table ${insert.table_name().text}",
          resolution.values
      )
    })

    if (insert.K_DEFAULT() != null) {
      // No validation needed for default value inserts.
    }

    if (insert.with_clause() != null) {
      try {
        resolver = resolver.withResolver(insert.with_clause())
      } catch (e: SqlitePluginException) {
        response.add(ResolutionError.WithTableError(e.originatingElement, e.message))
      }
    }

    val valuesBeingInserted: Resolution
    if (insert.values() != null) {
      valuesBeingInserted = resolver.resolve(insert.values(), scopedValues)
    } else if (insert.select_stmt() != null) {
      valuesBeingInserted = resolver.resolve(insert.select_stmt())
    } else {
      valuesBeingInserted = Resolution()
    }
    response.addAll(valuesBeingInserted.errors)

    if (insert.K_DEFAULT() != null) {
      // Inserting default values, no need to check against column size.
      return response
    }

    val columnSize = if (insert.column_name().size > 0) insert.column_name().size else columnsForTable.size
    if (valuesBeingInserted.errors.isEmpty() && valuesBeingInserted.values.size != columnSize) {
      response.add(ResolutionError.InsertError(
          insert.select_stmt() ?: insert.values(), "Unexpected number of " +
          "values being inserted. found: ${valuesBeingInserted.values.size} expected: $columnSize"
      ))
    }

    return response
  }
}