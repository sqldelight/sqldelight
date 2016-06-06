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
import com.squareup.sqldelight.resolution.resolve
import com.squareup.sqldelight.types.Value
import java.util.ArrayList

internal class UpdateValidator(
    val resolver: Resolver,
    val scopedValues: List<Value> = emptyList()
) {
  fun validate(update: SqliteParser.Update_stmt_limitedContext) : List<ResolutionError> {
    val resolution = resolver.resolve(update.qualified_table_name().table_name())
    val response = ArrayList(resolution.errors)
    response.addAll(update.column_name().flatMap { resolver.resolve(resolution.values, it).errors })

    var resolver: Resolver
    if (update.with_clause() != null) {
      try {
        resolver = this.resolver.withResolver(update.with_clause())
      } catch (e: SqlitePluginException) {
        response.add(ResolutionError.WithTableError(e.originatingElement, e.message))
        resolver = this.resolver
      }
    } else {
      resolver = this.resolver
    }

    resolver = resolver.withScopedValues(scopedValues + resolution.values)
    response.addAll(update.expr().flatMap { resolver.resolve(it, false).errors })
    response.addAll(update.ordering_term().flatMap { resolver.resolve(it.expr(), false).errors })

    return response
  }

  fun validate(update: SqliteParser.Update_stmtContext) : List<ResolutionError> {
    val resolution = resolver.resolve(update.table_name())
    val response = ArrayList(resolution.errors)
    response.addAll(update.column_name().flatMap { resolver.resolve(resolution.values, it).errors })

    var resolver: Resolver
    if (update.with_clause() != null) {
      try {
        resolver = this.resolver.withResolver(update.with_clause())
      } catch (e: SqlitePluginException) {
        response.add(ResolutionError.WithTableError(e.originatingElement, e.message))
        resolver = this.resolver
      }
    } else {
      resolver = this.resolver
    }

    resolver = resolver.withScopedValues(scopedValues + resolution.values)
    response.addAll(update.expr().flatMap { resolver.resolve(it).errors })

    return response
  }
}