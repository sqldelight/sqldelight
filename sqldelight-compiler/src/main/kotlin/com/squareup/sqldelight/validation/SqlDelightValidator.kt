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
import com.squareup.sqldelight.types.ResolutionError
import com.squareup.sqldelight.types.Resolver
import com.squareup.sqldelight.types.SymbolTable
import java.util.ArrayList

class SqlDelightValidator {
  fun validate(
      parse: SqliteParser.ParseContext,
      symbolTable: SymbolTable
  ): Status.ValidationStatus {
    val resolver = Resolver(symbolTable)
    val errors = ArrayList<ResolutionError>()

    val columnNames = linkedSetOf<String>()
    val sqlStatementNames = linkedSetOf<String>()

    parse.sql_stmt_list().create_table_stmt()?.let { createTable ->
      errors.addAll(CreateTableValidator(resolver).validate(createTable))

      createTable.column_def().forEach { column ->
        if (!columnNames.add(column.column_name().text)) {
          errors.add(ResolutionError.CreateTableError(
              column.column_name(), "Duplicate column name"
          ))
        }
      }
    }

    parse.sql_stmt_list().sql_stmt().forEach { sqlStmt ->
      if (columnNames.contains(sqlStmt.sql_stmt_name().text)) {
        errors.add(ResolutionError.CollisionError(
            sqlStmt.sql_stmt_name(), "SQL identifier collides with column name"
        ))
      }
      if (!sqlStatementNames.add(sqlStmt.sql_stmt_name().text)) {
        errors.add(ResolutionError.CollisionError(
            sqlStmt.sql_stmt_name(), "Duplicate SQL identifier"
        ))
      }
      sqlStmt.apply {
        select_stmt()?.let { errors.addAll(resolver.resolve(it).errors) }
        insert_stmt()?.let { errors.addAll(InsertValidator(resolver).validate(it)) }
        update_stmt()?.let { errors.addAll(UpdateValidator(resolver).validate(it)) }
        update_stmt_limited()?.let { errors.addAll(UpdateValidator(resolver).validate(it)) }
        delete_stmt()?.let { errors.addAll(DeleteValidator(resolver).validate(it)) }
        delete_stmt_limited()?.let { errors.addAll(DeleteValidator(resolver).validate(it)) }
        create_index_stmt()?.let { errors.addAll(CreateIndexValidator(resolver).validate(it)) }
        create_trigger_stmt()?.let { errors.addAll(CreateTriggerValidator(resolver).validate(it)) }
      }
    }

    return if (errors.isEmpty())
      Status.ValidationStatus.Validated(parse, resolver.dependencies)
    else
      Status.ValidationStatus.Invalid(errors, resolver.dependencies)
  }

  companion object {
    const val ALL_FILE_DEPENDENCY = "all_file_dependency"
  }
}