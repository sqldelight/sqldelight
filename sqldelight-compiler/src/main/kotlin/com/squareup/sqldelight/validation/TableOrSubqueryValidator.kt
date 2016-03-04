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

internal class TableOrSubqueryValidator(
    private val resolver: Resolver,
    private val values: List<Value>,
    private val scopedValues: List<Value>
) {
  fun validate(tableOrSubquery: SqliteParser.Table_or_subqueryContext) {
    if (tableOrSubquery.table_name() != null) {
      // : ( database_name '.' )? table_name ( K_AS? table_alias )?
      //   ( K_INDEXED K_BY index_name
      //   | K_NOT K_INDEXED )?
      // No validation needed.
      return
    }
    if (tableOrSubquery.table_or_subquery() != null) {
      // | '(' ( table_or_subquery ( ',' table_or_subquery )*
      tableOrSubquery.table_or_subquery().forEach { validate(it) }
      return
    }
    if (tableOrSubquery.join_clause() != null) {
      // | join_clause )
      JoinValidator(resolver, values, scopedValues).validate(tableOrSubquery.join_clause())
      return
    }
    if (tableOrSubquery.select_stmt() != null) {
      // | '(' select_stmt ')' ( K_AS? table_alias )?
      SelectStmtValidator(resolver, values + scopedValues).validate(tableOrSubquery.select_stmt())
      return
    }
  }
}
