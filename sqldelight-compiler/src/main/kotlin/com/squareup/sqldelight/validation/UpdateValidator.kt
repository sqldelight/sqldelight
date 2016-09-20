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
import com.squareup.sqldelight.resolution.ResolutionError
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.query.Result
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.resolution.resolve
import com.squareup.sqldelight.types.ArgumentType

internal class UpdateValidator(
    val resolver: Resolver,
    val scopedValues: List<Result> = emptyList()
) {
  fun validate(update: SqliteParser.Update_stmtContext) {
    val resolution = listOf(resolver.resolve(update.table_name())).filterNotNull()

    var subResolver: Resolver
    if (update.with_clause() != null) {
      try {
        subResolver = this.resolver.withResolver(update.with_clause())
      } catch (e: SqlitePluginException) {
        subResolver = this.resolver
        subResolver.errors.add(ResolutionError.WithTableError(e.originatingElement, e.message))
      }
    } else {
      subResolver = this.resolver
    }

    subResolver = subResolver.withScopedValues(scopedValues + resolution)

    update.expr()?.let { subResolver.resolve(it, false, ArgumentType.boolean(it)) }
    update.column_name().zip(update.setter_expr(), { column, setter ->
      val columnValue = resolver.resolve(resolution, column) as? Value
      subResolver.resolve(setter.expr(), expectedType = ArgumentType.SingleValue(columnValue))
    })
  }
}
