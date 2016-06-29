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
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Result
import com.squareup.sqldelight.resolution.resolve

internal class CreateTriggerValidator(val resolver: Resolver) {
  fun validate(trigger: SqliteParser.Create_trigger_stmtContext) {
    val resolution = listOf(resolver.resolve(trigger.table_name())).filterNotNull()
    trigger.column_name().forEach { resolver.resolve(resolution, it) }

    val availableColumns = availableColumns(trigger, resolution)

    if (trigger.expr() != null) {
      resolver.withScopedValues(availableColumns).resolve(trigger.expr())
    }

    trigger.select_stmt().forEach { resolver.resolve(it) }
    trigger.insert_stmt().forEach { InsertValidator(resolver, availableColumns).validate(it) }
    trigger.delete_stmt().forEach { DeleteValidator(resolver, availableColumns).validate(it) }
    trigger.update_stmt().forEach { UpdateValidator(resolver, availableColumns).validate(it) }
  }

  fun availableColumns(
      trigger: SqliteParser.Create_trigger_stmtContext,
      tableValues: List<Result>
  ): List<Result> {
    if (trigger.K_INSERT() != null) {
      return tableValues + QueryResults(trigger, tableValues).copy(name = "new")
    }
    if (trigger.K_UPDATE() != null) {
      return tableValues + QueryResults(trigger, tableValues).copy(name = "old") +
          QueryResults(trigger, tableValues).copy(name = "new")
    }
    if (trigger.K_DELETE() != null) {
      return tableValues + QueryResults(trigger, tableValues).copy(name = "old")
    }

    throw IllegalStateException("Did not know how to handle create trigger statement")
  }
}
