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

internal class UpdateValidator(
    val resolver: Resolver,
    val scopedValues: List<Value> = emptyList()
) {
  fun validate(update: SqliteParser.Update_stmt_limitedContext) {
    val tableColumns = resolver.resolve(update.qualified_table_name().table_name())

    update.column_name().forEach { column ->
      if (tableColumns.filter({ it.columnName == column.text }).isEmpty()) {
        throw SqlitePluginException(column,
            "Column ${column.text} does not exist in table ${update.qualified_table_name().table_name().text}")
      }
    }

    val resolver: Resolver
    if (update.with_clause() != null) {
      resolver = this.resolver.withResolver(update.with_clause())
    } else {
      resolver = this.resolver
    }


    val expressionValidator = ExpressionValidator(resolver, tableColumns + scopedValues)
    update.expr().forEach { expressionValidator.validate(it) }

    val orderingValidator = OrderingTermValidator(resolver, tableColumns)
    update.ordering_term().forEach { orderingValidator.validate(it) }
  }

  fun validate(update: SqliteParser.Update_stmtContext) {
    val tableColumns = resolver.resolve(update.table_name())

    update.column_name().forEach { column ->
      if (tableColumns.filter({ it.columnName == column.text }).isEmpty()) {
        throw SqlitePluginException(column,
            "Column ${column.text} does not exist in table ${update.table_name().text}")
      }
    }

    val resolver: Resolver
    if (update.with_clause() != null) {
      resolver = this.resolver.withResolver(update.with_clause())
    } else {
      resolver = this.resolver
    }


    val expressionValidator = ExpressionValidator(resolver, tableColumns + scopedValues)
    update.expr().forEach { expressionValidator.validate(it) }
  }
}