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

internal class UpdateValidator(
    val resolver: Resolver,
    val scopedValues: List<Value> = emptyList()
) {
  fun validate(update: SqliteParser.Update_stmt_limitedContext) : List<ResolutionError> {
    val resolution = resolver.resolve(update.qualified_table_name().table_name())
    val response = ArrayList(resolution.errors)

    update.column_name().forEach { column ->
      if (resolution.values.filter({ it.columnName == column.text }).isEmpty()) {
        response.add(ResolutionError.ColumnNameNotFound(
            column,
            "Column ${column.text} does not exist in table ${update.qualified_table_name().table_name().text}",
            resolution.values
        ))
      }
    }

    val resolver: Resolver
    if (update.with_clause() != null) {
      resolver = this.resolver.withResolver(update.with_clause())
    } else {
      resolver = this.resolver
    }


    val expressionValidator = ExpressionValidator(resolver, resolution.values + scopedValues)
    response.addAll(update.expr().flatMap { expressionValidator.validate(it) })

    val orderingValidator = OrderingTermValidator(resolver, resolution.values)
    response.addAll(update.ordering_term().flatMap { orderingValidator.validate(it) })

    return response
  }

  fun validate(update: SqliteParser.Update_stmtContext) : List<ResolutionError> {
    val resolution = resolver.resolve(update.table_name())
    val response = ArrayList(resolution.errors)

    update.column_name().forEach { column ->
      if (resolution.values.filter({ it.columnName == column.text }).isEmpty()) {
        response.add(ResolutionError.ColumnNameNotFound(
            column,
            "Column ${column.text} does not exist in table ${update.table_name().text}",
            resolution.values
        ))
      }
    }

    val resolver: Resolver
    if (update.with_clause() != null) {
      resolver = this.resolver.withResolver(update.with_clause())
    } else {
      resolver = this.resolver
    }

    val expressionValidator = ExpressionValidator(resolver, resolution.values + scopedValues)
    response.addAll(update.expr().flatMap { expressionValidator.validate(it) })

    return response
  }
}