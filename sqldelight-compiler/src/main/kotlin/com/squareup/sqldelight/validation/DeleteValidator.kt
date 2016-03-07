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
import com.squareup.sqldelight.types.Resolver

internal class DeleteValidator(val resolver: Resolver) {
  fun validate(delete: SqliteParser.Delete_stmt_limitedContext) {
    val tableColumns = resolver.resolve(delete.qualified_table_name().table_name())

    val resolver: Resolver
    if (delete.with_clause() != null) {
      resolver = this.resolver.withResolver(delete.with_clause())
    } else {
      resolver = this.resolver
    }

    val expressionValidator = ExpressionValidator(resolver, tableColumns)
    delete.expr().forEach { expressionValidator.validate(it) }

    val orderingValidator = OrderingTermValidator(resolver, tableColumns)
    delete.ordering_term().forEach { orderingValidator.validate(it) }
  }

  fun validate(delete: SqliteParser.Delete_stmtContext) {
    val tableColumns = resolver.resolve(delete.table_name())

    val resolver: Resolver
    if (delete.with_clause() != null) {
      resolver = this.resolver.withResolver(delete.with_clause())
    } else {
      resolver = this.resolver
    }

    if (delete.expr() != null) {
      ExpressionValidator(resolver, tableColumns).validate(delete.expr())
    }
  }
}