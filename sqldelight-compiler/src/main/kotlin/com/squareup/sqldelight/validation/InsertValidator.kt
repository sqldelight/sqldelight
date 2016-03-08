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

internal class InsertValidator(
    val resolver: Resolver,
    val scopedValues: List<Value> = emptyList()
) {
  fun validate(insert: SqliteParser.Insert_stmtContext) {
    val columnsForTable = resolver.resolve(insert.table_name()).map { it.columnName }
    insert.column_name().filter({ !columnsForTable.contains(it.text) }).forEach {
      throw SqlitePluginException(it,
          "Column ${it.text} does not exist in table ${insert.table_name().text}")
    }

    if (insert.K_DEFAULT() != null) {
      // No validation needed for default value inserts.
    }

    val valuesBeingInserted = resolver.resolve(insert, scopedValues)
    val columnSize = if (insert.column_name().size > 0) insert.column_name().size else columnsForTable.size
    if (valuesBeingInserted.size != columnSize) {
      throw SqlitePluginException(insert.select_stmt() ?: insert.values(), "Unexpected number of " +
          "values being inserted. found: ${valuesBeingInserted.size} expected: $columnSize")
    }
  }
}