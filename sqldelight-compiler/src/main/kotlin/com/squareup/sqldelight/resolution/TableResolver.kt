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
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Result
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.types.ForeignKey
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Take a table or subquery rule and return a list of the selected values.
 */
internal fun Resolver.resolve(tableOrSubquery: SqliteParser.Table_or_subqueryContext): List<Result> {
  if (tableOrSubquery.table_name() != null) {
    val table = resolve(tableOrSubquery.table_name()) ?: return emptyList()
    if (tableOrSubquery.table_alias() != null) {
      when (table) {
        is Table -> return listOf(table.copy(
            name = tableOrSubquery.table_alias().text,
            element = tableOrSubquery.table_alias()
        ))
        is QueryResults -> return listOf(table.copy(
            name = tableOrSubquery.table_alias().text,
            element = tableOrSubquery.table_alias()
        ))
        else -> throw IllegalStateException("Unexpected result $table")
      }
    } else {
      return listOf(table)
    }
  } else if (tableOrSubquery.select_stmt() != null) {
    val results = resolve(tableOrSubquery.select_stmt())
    if (tableOrSubquery.table_alias() != null) {
      return listOf(QueryResults(tableOrSubquery.table_alias(), results))
    } else {
      return results
    }
  } else if (tableOrSubquery.table_or_subquery().size > 0) {
    return tableOrSubquery.table_or_subquery().flatMap { resolve(it) }
  } else if (tableOrSubquery.join_clause() != null) {
    return resolve(tableOrSubquery.join_clause())
  } else {
    errors.add(ResolutionError.IncompleteRule(tableOrSubquery, "Missing table or subquery"))
    return emptyList()
  }
}

internal fun Resolver.resolve(
    results: List<Result>,
    columnName: SqliteParser.Column_nameContext,
    tableName: SqliteParser.Table_nameContext? = null,
    errorText: String = "No column found with name ${columnName.text}"
): Result? {
  val matchingColumns = results.flatMap { it.findElement(columnName.text, tableName?.text) }
  if (matchingColumns.isEmpty()) {
    if (tableName == null) {
      errors.add(ResolutionError.ColumnOrTableNameNotFound(columnName,
          errorText, results, tableName?.text))
    } else {
      errors.add(ResolutionError.ColumnNameNotFound(columnName,
          errorText, results))
    }
  } else if (matchingColumns.size > 1) {
    errors.add(ResolutionError.ExpressionError(columnName, "Ambiguous column name ${columnName.text}"))
  } else {
    findElementAtCursor(tableName, results.filter {
      it.findElement(columnName.text, tableName?.text).isNotEmpty()
    }.firstOrNull()?.element, elementToFind)
    findElementAtCursor(columnName, matchingColumns.first().element, elementToFind)
    return matchingColumns.single()
  }
  return null
}

internal fun Resolver.resolve(tableName: SqliteParser.Table_nameContext) = resolveParse(tableName)

internal fun Resolver.resolve(qualifiedTableName: SqliteParser.Qualified_table_nameContext)
    = resolveParse(qualifiedTableName)

private fun Resolver.resolveParse(tableName: ParserRuleContext, tableOnly: Boolean  = false): Result? {
  val createTable = symbolTable.tables[tableName.text]
  if (createTable != null) {
    dependencies.add(symbolTable.tableTags.getForValue(tableName.text))
    findElementAtCursor(tableName, createTable.table_name(), elementToFind)
    if (createTable.select_stmt() != null) {
      return QueryResults(
          createTable.table_name(),
          resolve(createTable.select_stmt()),
          symbolTable.tableTypes[createTable.table_name().text]!!
      )
    }
    return resolve(createTable)
  }

  if (tableOnly) {
    // If table was missing we add a dependency on all future files.
    dependencies.add(SqlDelightValidator.ALL_FILE_DEPENDENCY)

    errors.add(ResolutionError.TableNameNotFound(
        tableName, "Cannot find table ${tableName.text}", symbolTable.tables.keys
    ))
    return null
  }

  val view = symbolTable.views[tableName.text]
  if (view != null) {
    dependencies.add(symbolTable.viewTags.getForValue(tableName.text))
    findElementAtCursor(tableName, view.view_name(), elementToFind)
    if (!currentlyResolvingViews.add(view.view_name().text)) {
      val chain = currentlyResolvingViews.joinToString(" -> ")
      errors.add(ResolutionError.RecursiveResolution(view.view_name(),
          "Recursive subquery found: $chain -> ${view.view_name().text}"))
      return null
    }
    val results = copy(elementToFind = null).resolve(view.select_stmt())
    currentlyResolvingViews.remove(view.view_name().text)

    // While we resolve the view we shouldn't look for an element so create a new resolver.
    return QueryResults(
        view.view_name(), results, symbolTable.tableTypes[view.view_name().text]!!, true
    )
  }

  val commonTable = symbolTable.commonTables[tableName.text]
  if (commonTable != null) {
    var resolution = resolve(commonTable.select_stmt())
    if (commonTable.column_name().size > 0) {
      // Keep the errors from the original resolution but only the values that
      // are specified in the column_name() array.
      resolution = commonTable.column_name().map { resolve(resolution, it, null) }.filterNotNull()
    }
    findElementAtCursor(tableName, commonTable.table_name(), elementToFind)
    return QueryResults(tableName, resolution)
  }

  // If table was missing we add a dependency on all future files.
  dependencies.add(SqlDelightValidator.ALL_FILE_DEPENDENCY)

  errors.add(ResolutionError.TableNameNotFound(tableName,
      "Cannot find table or view ${tableName.text}",
      symbolTable.commonTables.keys + symbolTable.tables.keys + symbolTable.views.keys
  ))
  return null
}

internal fun Resolver.resolve(createTable: SqliteParser.Create_table_stmtContext) =
    Table(createTable, symbolTable)

internal fun Resolver.foreignKeys(foreignTable: SqliteParser.Foreign_tableContext): ForeignKey {
  return ForeignKey.findForeignKeys(symbolTable, resolveParse(foreignTable, true) as? Table)
}
