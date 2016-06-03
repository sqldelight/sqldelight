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
package com.squareup.sqldelight.resolution

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.types.ForeignKey
import com.squareup.sqldelight.types.Value
import com.squareup.sqldelight.types.columns
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Take a table or subquery rule and return a list of the selected values.
 */
internal fun Resolver.resolve(tableOrSubquery: SqliteParser.Table_or_subqueryContext): Resolution {
  var resolution: Resolution
  if (tableOrSubquery.table_name() != null) {
    resolution = resolve(tableOrSubquery.table_name())
  } else if (tableOrSubquery.select_stmt() != null) {
    resolution = resolve(tableOrSubquery.select_stmt())
  } else if (tableOrSubquery.table_or_subquery().size > 0) {
    resolution = tableOrSubquery.table_or_subquery().fold(
        Resolution()) { response, table_or_subquery ->
      response + resolve(table_or_subquery)
    }
  } else if (tableOrSubquery.join_clause() != null) {
    resolution = resolve(tableOrSubquery.join_clause())
  } else {
    return Resolution(ResolutionError.IncompleteRule(tableOrSubquery, "Missing table or subquery"))
  }

  // Alias the values if an alias was given.
  if (tableOrSubquery.table_alias() != null) {
    resolution = resolution.copy(resolution.values.map {
      it.copy(tableName = tableOrSubquery.table_alias().text, tableNameElement = tableOrSubquery.table_alias())
    })
  }

  return resolution
}

internal fun Resolver.resolve(
    availableColumns: List<Value>,
    columnName: SqliteParser.Column_nameContext,
    tableName: SqliteParser.Table_nameContext? = null
): Resolution {
  val matchingColumns = availableColumns.columns(columnName.text, tableName?.text)
  if (matchingColumns.isEmpty()) {
    return Resolution(ResolutionError.ColumnOrTableNameNotFound(columnName,
        "No column found with name ${columnName.text}", availableColumns, tableName?.text))
  } else if (matchingColumns.size > 1) {
    return Resolution(ResolutionError.ExpressionError(columnName, "Ambiguous column name " +
        "${columnName.text}, found in tables ${matchingColumns.map { it.tableName }}"))
  } else {
    return Resolution(matchingColumns)
        .findElement(tableName, matchingColumns.first().tableNameElement, elementToFind)
        .findElement(columnName, matchingColumns.first().element, elementToFind)
  }
}

internal fun Resolver.resolve(tableName: SqliteParser.Table_nameContext) = resolveParse(tableName)

internal fun Resolver.resolve(qualifiedTableName: SqliteParser.Qualified_table_nameContext)
    = resolveParse(qualifiedTableName)

private fun Resolver.resolveParse(tableName: ParserRuleContext): Resolution {
  val createTable = symbolTable.tables[tableName.text]
  if (createTable != null) {
    dependencies.add(symbolTable.tableTags.getForValue(tableName.text))
    if (createTable.select_stmt() != null) {
      return resolve(createTable.select_stmt()).findElement(tableName,
          createTable.table_name(), elementToFind)
    }
    return resolve(createTable).findElement(tableName, createTable.table_name(), elementToFind)
  }

  val view = symbolTable.views[tableName.text]
  if (view != null) {
    dependencies.add(symbolTable.viewTags.getForValue(tableName.text))
    if (!currentlyResolvingViews.add(view.view_name().text)) {
      val chain = currentlyResolvingViews.joinToString(" -> ")
      return Resolution(ResolutionError.RecursiveResolution(view.view_name(),
          "Recursive subquery found: $chain -> ${view.view_name().text}"))
    }

    // While we resolve the view we shouldn't look for an element so create a new resolver.
    val originalResult = copy(elementToFind = null).resolve(view.select_stmt())
    val result = originalResult.copy(values = originalResult.values.map {
      it.copy(tableName = view.view_name().text, tableNameElement = view.view_name())
    })
    currentlyResolvingViews.remove(view.view_name().text)
    return result.findElement(tableName, view.view_name(), elementToFind)
  }

  val commonTable = symbolTable.commonTables[tableName.text]
  if (commonTable != null) {
    var resolution = resolve(commonTable.select_stmt())
    if (commonTable.column_name().size > 0) {
      // Keep the errors from the original resolution but only the values that
      // are specified in the column_name() array.
      resolution = Resolution(errors = resolution.errors) + commonTable.column_name()
          .fold(Resolution()) { response, column_name ->
            val found = resolution.values.columns(column_name.text, null)
            if (found.size == 0) {
              return@fold response + Resolution(ResolutionError.ColumnNameNotFound(
                  column_name,
                  "No column found in common table with name ${column_name.text}",
                  resolution.values
              ))
            }
            val originalResponse = Resolution(found)
            return@fold response + originalResponse.copy(values = originalResponse.values.map {
              it.copy(tableName = tableName.text, tableNameElement = tableName)
            }).findElement(column_name, found[0].element, elementToFind)
          }
    }
    return resolution.copy(values = resolution.values.map {
      it.copy(tableName = tableName.text)
    }).findElement(tableName, commonTable.table_name(), elementToFind)
  }

  // If table was missing we add a dependency on all future files.
  dependencies.add(SqlDelightValidator.ALL_FILE_DEPENDENCY)

  return Resolution(ResolutionError.TableNameNotFound(tableName,
      "Cannot find table or view ${tableName.text}",
      symbolTable.commonTables.keys + symbolTable.tables.keys + symbolTable.views.keys
  ))
}

internal fun Resolver.resolve(createTable: SqliteParser.Create_table_stmtContext) =
    Resolution(createTable.column_def().map { Value(createTable.table_name(), it) })

fun Resolver.foreignKeys(foreignTable: SqliteParser.Foreign_tableContext): ForeignKey {
  return ForeignKey.findForeignKeys(foreignTable, symbolTable, resolveParse(foreignTable).values)
}