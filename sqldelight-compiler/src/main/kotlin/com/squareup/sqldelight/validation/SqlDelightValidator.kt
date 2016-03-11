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
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.types.Resolver
import com.squareup.sqldelight.types.SymbolTable
import org.antlr.v4.runtime.misc.Interval

class SqlDelightValidator {
  fun validate(
      parse: SqliteParser.ParseContext,
      symbolTable: SymbolTable
  ): Status.ValidationStatus {
    val resolver = Resolver(symbolTable)
    val exceptions = linkedMapOf<Interval, SqlitePluginException>()

    val columnNames = linkedSetOf<String>()
    val sqlStatementNames = linkedSetOf<String>()

    parse.sql_stmt_list().create_table_stmt()?.let { createTable ->
      try {
        CreateTableValidator(resolver).validate(createTable)

        createTable.column_def().forEach { column ->
          if (column.column_name().text.equals("create_table", true)) {
            throw SqlitePluginException(column.column_name(), "Column name 'create_table' forbidden")
          }
          if (!columnNames.add(column.column_name().text)) {
            throw SqlitePluginException(column.column_name(), "Duplicate column name")
          }
        }
      } catch (e: SqlitePluginException) {
        exceptions.put(e.originatingElement.sourceInterval, e)
      }
    }

    parse.sql_stmt_list().sql_stmt().forEach { sqlStmt ->
      try {
        if (columnNames.contains(sqlStmt.sql_stmt_name().text)) {
          throw SqlitePluginException(sqlStmt.sql_stmt_name(),
              "SQL identifier collides with column name")
        }
        if (!sqlStatementNames.add(sqlStmt.sql_stmt_name().text)) {
          throw SqlitePluginException(sqlStmt.sql_stmt_name(), "Duplicate SQL identifier")
        }

        if (sqlStmt.select_stmt() != null) {
          // TODO: Take the returned columns and turn them into a mapper.
          resolver.resolve(sqlStmt.select_stmt())
        }
        if (sqlStmt.insert_stmt() != null) {
          InsertValidator(resolver).validate(sqlStmt.insert_stmt())
        }
        if (sqlStmt.update_stmt() != null) {
          UpdateValidator(resolver).validate(sqlStmt.update_stmt())
        }
        if (sqlStmt.update_stmt_limited() != null) {
          UpdateValidator(resolver).validate(sqlStmt.update_stmt_limited())
        }
        if (sqlStmt.delete_stmt() != null) {
          DeleteValidator(resolver).validate(sqlStmt.delete_stmt())
        }
        if (sqlStmt.delete_stmt_limited() != null) {
          DeleteValidator(resolver).validate(sqlStmt.delete_stmt_limited())
        }
        if (sqlStmt.create_index_stmt() != null) {
          CreateIndexValidator(resolver).validate(sqlStmt.create_index_stmt())
        }
        if (sqlStmt.create_trigger_stmt() != null) {
          CreateTriggerValidator(resolver).validate(sqlStmt.create_trigger_stmt())
        }
      } catch (e: SqlitePluginException) {
        exceptions.put(e.originatingElement.sourceInterval, e)
      }
    }

    return if (exceptions.isEmpty())
      Status.ValidationStatus.Validated(parse, resolver.dependencies)
    else
      Status.ValidationStatus.Invalid(exceptions.values, resolver.dependencies)
  }

  companion object {
    const val ALL_FILE_DEPENDENCY = "all_file_dependency"
  }
}