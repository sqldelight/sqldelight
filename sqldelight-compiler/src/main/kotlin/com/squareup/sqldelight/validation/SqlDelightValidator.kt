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
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.types.Resolver
import com.squareup.sqldelight.types.SymbolTable

class SqlDelightValidator {
  fun validate(
      parse: SqliteParser.ParseContext,
      symbolTable: SymbolTable
  ): Status {
    for (sqlStmt in parse.sql_stmt_list().sql_stmt()) {
      if (sqlStmt.select_stmt() != null) {
        SelectStmtValidator(Resolver(symbolTable), emptyList()).validate(sqlStmt.select_stmt())
      }
    }

    return Status.Validated(parse)
  }
}