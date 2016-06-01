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
import com.squareup.sqldelight.resolution.ResolutionError
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.resolve
import java.util.ArrayList

internal class CreateIndexValidator(val resolver: Resolver) {
  fun validate(index: SqliteParser.Create_index_stmtContext) : List<ResolutionError> {
    val resolution = resolver.resolve(index.table_name())
    val response = ArrayList(resolution.errors)

    index.indexed_column().forEach { column ->
      if (resolution.values.filter({ it.columnName == column.column_name().text}).isEmpty()) {
        response.add(ResolutionError.ColumnNameNotFound(
            column.column_name(),
            "Column ${column.column_name().text} does not exist in table ${index.table_name().text}",
            resolution.values
        ))
      }
    }

    if (index.expr() != null) {
      response.addAll(ExpressionValidator(resolver, resolution.values).validate(index.expr()))
    }

    return response
  }
}