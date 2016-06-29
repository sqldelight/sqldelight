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
import com.squareup.sqldelight.resolution.resolve

internal class DeleteValidator(
    val resolver: Resolver,
    val scopedValues: List<Result> = emptyList()
) {
  fun validate(delete: SqliteParser.Delete_stmt_limitedContext) {
    val resolution = listOf(resolver.resolve(delete.qualified_table_name().table_name()))
        .filterNotNull()

    val resolver: Resolver
    if (delete.with_clause() != null) {
      try {
        resolver = this.resolver.withResolver(delete.with_clause())
      } catch (e: SqlitePluginException) {
        resolver = this.resolver
        resolver.errors.add(ResolutionError.WithTableError(e.originatingElement, e.message))
      }
    } else {
      resolver = this.resolver
    }

    val scopedResolver = resolver.withScopedValues(scopedValues + resolution)
    delete.ordering_term().map { it.expr() }.plus(delete.expr()).forEach {
      scopedResolver.resolve(it, false)
    }
  }

  fun validate(delete: SqliteParser.Delete_stmtContext) {
    val resolution = listOf(resolver.resolve(delete.table_name())).filterNotNull()

    val resolver: Resolver
    if (delete.with_clause() != null) {
      try {
        resolver = this.resolver.withResolver(delete.with_clause())
      } catch (e: SqlitePluginException) {
        resolver = this.resolver
        resolver.errors.add(ResolutionError.WithTableError(e.originatingElement, e.message))
      }
    } else {
      resolver = this.resolver
    }

    if (delete.expr() != null) {
      resolver.withScopedValues(scopedValues + resolution).resolve(delete.expr())
    }
  }
}
