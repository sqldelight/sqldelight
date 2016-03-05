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
import com.squareup.sqldelight.types.Value

internal class SelectStmtValidator(
    private val resolver: Resolver,
    private val values: List<Value>
) {
  fun validate(selectStmt: SqliteParser.Select_stmtContext) {
    if (selectStmt.ordering_term().size > 0) {
      val validator = OrderingTermValidator(resolver, values)
      selectStmt.ordering_term().forEach { validator.validate(it) }
    }

    if (selectStmt.K_LIMIT() != null) {
      val validator = ExpressionValidator(resolver, values)
      selectStmt.expr().forEach { validator.validate(it) }
    }
  }
}
