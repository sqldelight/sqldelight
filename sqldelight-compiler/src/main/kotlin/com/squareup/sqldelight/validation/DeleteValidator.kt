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

internal class DeleteValidator(
    val resolver: Resolver,
    val scopedValues: List<Value> = emptyList()
) {
  fun validate(delete: SqliteParser.Delete_stmt_limitedContext) : List<ResolutionError> {
    val resolution = resolver.resolve(delete.qualified_table_name().table_name())
    val response = ArrayList(resolution.errors)

    val resolver: Resolver
    if (delete.with_clause() != null) {
      try {
        resolver = this.resolver.withResolver(delete.with_clause())
      } catch (e: SqlitePluginException) {
        response.add(ResolutionError.WithTableError(e.originatingElement, e.message))
        resolver = this.resolver
      }
    } else {
      resolver = this.resolver
    }

    val expressionValidator = ExpressionValidator(resolver, resolution.values + scopedValues)
    response.addAll(delete.expr().flatMap { expressionValidator.validate(it) })

    val orderingValidator = OrderingTermValidator(resolver, resolution.values)
    response.addAll(delete.ordering_term().flatMap { orderingValidator.validate(it) })

    return response
  }

  fun validate(delete: SqliteParser.Delete_stmtContext) : List<ResolutionError> {
    val resolution = resolver.resolve(delete.table_name())
    val response = ArrayList(resolution.errors)

    val resolver: Resolver
    if (delete.with_clause() != null) {
      try {
        resolver = this.resolver.withResolver(delete.with_clause())
      } catch (e: SqlitePluginException) {
        response.add(ResolutionError.WithTableError(e.originatingElement, e.message))
        resolver = this.resolver
      }
    } else {
      resolver = this.resolver
    }

    if (delete.expr() != null) {
      response.addAll(ExpressionValidator(resolver, resolution.values + scopedValues)
          .validate(delete.expr()))
    }

    return response
  }
}