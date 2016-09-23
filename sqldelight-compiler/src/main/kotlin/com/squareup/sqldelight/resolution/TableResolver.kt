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
import com.squareup.sqldelight.resolution.query.resultColumnSize
import com.squareup.sqldelight.types.ForeignKey
import com.squareup.sqldelight.util.isRecursive
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Take a table or subquery rule and return a list of the selected values.
 */
internal fun Resolver.resolve(
    tableOrSubquery: SqliteParser.Table_or_subqueryContext,
    recursiveCommonTable: Pair<SqliteParser.Table_nameContext, List<Result>>? = null
): List<Result> {
  if (tableOrSubquery.table_name() != null) {
    val table: Result
    if (recursiveCommonTable != null && recursiveCommonTable.first.text == tableOrSubquery.table_name().text) {
      table = QueryResults(recursiveCommonTable.first, recursiveCommonTable.second)
    } else {
      table = resolve(tableOrSubquery.table_name()) ?: return emptyList()
    }
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
    values: List<Result>,
    columnName: SqliteParser.Column_nameContext,
    tableName: SqliteParser.Table_nameContext? = null,
    errorText: String = "No column found with name ${columnName.text}"
) = scopedResolve(listOf(values), columnName, tableName, errorText)

internal fun Resolver.scopedResolve(
    values: List<List<Result>>,
    columnName: SqliteParser.Column_nameContext,
    tableName: SqliteParser.Table_nameContext? = null,
    errorText: String = "No column found with name ${columnName.text}"
): Result? {
  values.reversed().forEach { results ->
    val matchingColumns = results.flatMap { it.findElement(columnName.text, tableName?.text) }
    if (matchingColumns.size > 1) {
      errors.add(ResolutionError.ExpressionError(columnName, "Ambiguous column name ${columnName.text}"))
    } else if (matchingColumns.size == 1) {
      findElementAtCursor(tableName, results.filter {
        it.findElement(columnName.text, tableName?.text).isNotEmpty()
      }.firstOrNull()?.element, elementToFind)
      findElementAtCursor(columnName, matchingColumns.first().element, elementToFind)
      return matchingColumns.single()
    }
  }
  if (tableName == null) {
    errors.add(ResolutionError.ColumnOrTableNameNotFound(columnName,
        errorText, values.flatMap { it }, tableName?.text))
  } else {
    errors.add(ResolutionError.ColumnNameNotFound(columnName,
        errorText, values.flatMap { it }))
  }
  return null
}

internal fun Resolver.resolve(tableName: SqliteParser.Table_nameContext) = resolveParse(tableName)

internal fun Resolver.resolve(viewName: SqliteParser.View_nameContext): QueryResults =
    resolveParse(viewName) as QueryResults

internal fun Resolver.resolve(qualifiedTableName: SqliteParser.Qualified_table_nameContext)
    = resolveParse(qualifiedTableName)

private fun Resolver.resolveParse(tableName: ParserRuleContext, tableOnly: Boolean  = false): Result? {
  val createTable = symbolTable.tables[tableName.text]
  if (createTable != null) {
    dependencies.add(symbolTable.tableTags.getForValue(tableName.text))
    tableDependencies.add(tableName.text)
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
    val results = safeRecursion(view.view_name()) {
      copy(elementToFind = null).resolve(view.select_stmt())
    } ?: return null

    // While we resolve the view we shouldn't look for an element so create a new resolver.
    return QueryResults(
        view.view_name(), results, symbolTable.tableTypes[view.view_name().text]!!, true
    )
  }

  val commonTable = symbolTable.commonTables[tableName.text]
  if (commonTable != null) {
    val resolution = safeRecursion(commonTable.table_name()) {
      commonTableResolution(commonTable, resolve(
          commonTable.select_stmt(),
          if (commonTable.isRecursive()) commonTable else null
      ))
    } ?: return null
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

/**
 * Table resolution can be recursive, wrap calls to resolve to prevent stack overflows.
 */
internal fun <T> Resolver.safeRecursion(tableName: ParserRuleContext, action: () -> T): T? {
  if (!currentlyResolvingViews.add(tableName.text)) {
    val chain = currentlyResolvingViews.joinToString(" -> ")
    errors.add(ResolutionError.RecursiveResolution(tableName,
        "Recursive subquery found: $chain -> ${tableName.text}"))
    return null
  }
  val result = action()
  currentlyResolvingViews.remove(tableName.text)
  return result
}

internal fun Resolver.commonTableResolution(
    commonTable: SqliteParser.Common_table_expressionContext,
    resolution: List<Result>
): List<Result> {
  if (commonTable.column_name().size > 0) {
    if (commonTable.column_name().size == resolution.resultColumnSize()) {
      return resolution.flatMap { it.expand() }.zip(commonTable.column_name(), { value, column ->
        value.copy(
            name = column.text,
            element = column
        )
      })
    }
    errors.add(ResolutionError.WithTableError(commonTable, "Select has " +
        "${resolution.resultColumnSize()} columns and only " +
        "${commonTable.column_name().size} were aliased"))
  }
  return resolution
}

internal fun Resolver.resolve(createTable: SqliteParser.Create_table_stmtContext) =
    Table(createTable, symbolTable)

internal fun Resolver.foreignKeys(foreignTable: SqliteParser.Foreign_tableContext): ForeignKey {
  return ForeignKey.findForeignKeys(symbolTable, resolveParse(foreignTable, true) as? Table)
}
