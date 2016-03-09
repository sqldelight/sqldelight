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
import com.squareup.sqldelight.types.ForeignKey
import com.squareup.sqldelight.types.Resolver

internal class CreateTableValidator(val resolver: Resolver) {
  fun validate(createTable: SqliteParser.Create_table_stmtContext) {
    val columns = resolver.resolve(createTable)
    val exprValidator = ExpressionValidator(resolver, columns, false)

    createTable.column_def().forEach { column ->
      column.column_constraint().map { it.expr() }.filterNotNull().forEach {
        exprValidator.validate(it)
      }
    }

    (createTable.column_def().flatMap { it.column_constraint() }.map { it.expr() }.filterNotNull()
        + createTable.table_constraint().map { it.expr() }).filterNotNull().forEach {
      exprValidator.validate(it)
    }

    createTable.table_constraint().forEach { tableConstraint ->
      if (tableConstraint.expr() != null) {
        exprValidator.validate(tableConstraint.expr())
      }
      (tableConstraint.indexed_column().map { it.column_name() } + tableConstraint.column_name())
          .forEach {
            if (!columns.map { it.columnName }.contains(it.text)) {
              throw SqlitePluginException(it, "Column ${it.text} not found on table" +
                  " ${createTable.table_name().text}")
            }
          }
    }

    createTable.column_def().forEach {
      if (it.column_constraint().filter { it.K_PRIMARY_KEY() != null }.size > 1) {
        throw SqlitePluginException(it, "Column can only have one primary key on a column")
      }
      if (it.column_constraint().filter { it.K_UNIQUE() != null }.size > 1) {
        throw SqlitePluginException(it, "Column can only have one unique constraint on a column")
      }
    }

    createTable.column_def()
        .flatMap { it.column_constraint() }
        .map { it.foreign_key_clause() }
        .filterNotNull()
        .forEach {
          // The index can be supplied a few different ways:
          //   A. if no columns are supplied, the foreign index is the primary key constraints on the parent table.
          //   B. if the columns supplied have any unique constraint in them which follows the same
          //      collation as the table.

          if (it.column_name().size > 1) {
            throw SqlitePluginException(it, "Column can only reference a single foreign key")
          }

          val foreignTablePrimaryKeys = resolver.foreignKeys(it.foreign_table())
          if (it.column_name().size == 0) {
            // Must map to the foreign tables primary key which must be exactly one column long.
            if (foreignTablePrimaryKeys.primaryKey.size != 1) {
              throw SqlitePluginException(it, "Table ${it.foreign_table().text} has a composite" +
                  " primary key")
            }
            return@forEach
          }
          if (!foreignTablePrimaryKeys.hasIndexWithColumns(it.column_name().map { it.text })) {
            throw SqlitePluginException(it, "Table ${it.foreign_table().text} does not have a " +
                "unique index on column ${it.column_name(0).text}")
          }
        }

    createTable.table_constraint().filter { it.foreign_key_clause() != null }.forEach { constraint ->
      val foreignClause = constraint.foreign_key_clause()
      val foreignTablePrimaryKeys = resolver.foreignKeys(foreignClause.foreign_table())

      if (foreignClause.column_name().size == 0) {
        // Must exact match foreign table primary key index.
        if (foreignTablePrimaryKeys.primaryKey.size != constraint.column_name().size) {
          throw SqlitePluginException(foreignClause, "Foreign key constraint must match the" +
              " primary key of the foreign table exactly. Constraint has " +
              "${constraint.column_name().size} columns and foreign table primary key has " +
              "${foreignTablePrimaryKeys.primaryKey.size} columns")
        }
        return@forEach
      }
      if (!foreignTablePrimaryKeys.hasIndexWithColumns(
          foreignClause.column_name().map { it.text })) {
        throw SqlitePluginException(foreignClause, "Table ${foreignClause.foreign_table().text}" +
            " does not have a unique index on columns" +
            " ${foreignClause.column_name().map { it.text }}")
      }
    }
  }

  private fun ForeignKey.hasIndexWithColumns(columns: List<String>) =
      (uniqueConstraints + listOf(primaryKey)).any {
        columns.size == it.size && columns.containsAll(it.map { it.columnName })
      }
}