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
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.resolution.query.QueryResults
import com.squareup.sqldelight.resolution.query.Result
import com.squareup.sqldelight.resolution.query.Table
import com.squareup.sqldelight.resolution.query.resultColumnSize
import com.squareup.sqldelight.validation.SelectOrValuesValidator
import com.squareup.sqldelight.validation.SelectStmtValidator

internal fun Resolver.resolve(selectStmt: SqliteParser.Select_stmtContext): List<Result> {
  val resolver = if (selectStmt.with_clause() != null) {
    try {
      withResolver(selectStmt.with_clause())
    } catch (e: SqlitePluginException) {
      errors.add(ResolutionError.WithTableError(e.originatingElement, e.message))
      this
    }
  } else {
    this
  }

  val resolution = resolver.resolve(selectStmt.select_or_values(0), selectStmt)

  // Resolve other compound select statements and verify they have equivalent columns.
  selectStmt.select_or_values().drop(1).forEach {
    val compoundValues = resolver.resolve(it)
    if (compoundValues.resultColumnSize() != resolution.resultColumnSize()) {
      errors.add(ResolutionError.CompoundError(it,
          "Unexpected number of columns in compound statement found: " +
              "${compoundValues.resultColumnSize()} expected: ${resolution.resultColumnSize()}"))
    }
    // TODO modify type and nullability to handle all possible values in the union.
  }

  return resolution
}

/**
 * Takes a select_or_values rule and returns the columns selected.
 */
internal fun Resolver.resolve(
    selectOrValues: SqliteParser.Select_or_valuesContext,
    parentSelect: SqliteParser.Select_stmtContext? = null
): List<Result> {
  val resolution: List<Result>
  if (selectOrValues.K_VALUES() != null) {
    // No columns are available, only selected columns are returned.
    SelectOrValuesValidator(this, scopedValues).validate(selectOrValues)
    return resolve(selectOrValues.values())
  } else if (selectOrValues.join_clause() != null) {
    resolution = resolve(selectOrValues.join_clause())
  } else if (selectOrValues.table_or_subquery().size > 0) {
    resolution = selectOrValues.table_or_subquery().flatMap { resolve(it) }
  } else {
    resolution = emptyList()
  }

  // Validate the select or values has valid expressions before aliasing/selection.
  SelectOrValuesValidator(this, scopedValues + resolution).validate(selectOrValues)

  if (parentSelect != null) {
    SelectStmtValidator(this, scopedValues + resolution).validate(parentSelect)
  }

  return selectOrValues.result_column().flatMap { resolve(it, resolution) }
}

/**
 * Take in a list of available columns and return a list of selected columns.
 */
internal fun Resolver.resolve(
    resultColumn: SqliteParser.Result_columnContext,
    availableValues: List<Result>
): List<Result> {
  if (resultColumn.text.equals("*")) {
    // SELECT *
    return availableValues
  }
  if (resultColumn.table_name() != null) {
    // SELECT some_table.*
    val tables = availableValues.filter { it is Table || it is QueryResults }
    val result =  tables.filter { it.name == resultColumn.table_name().text }
    if (result.resultColumnSize() == 0) {
      errors.add(ResolutionError.TableNameNotFound(
          resultColumn.table_name(),
          "Table name ${resultColumn.table_name().text} not found",
          tables.map { it.name }.filterNotNull().distinct()))
      return emptyList()
    }
    findElementAtCursor(resultColumn.table_name(), tables.first().element, elementToFind)
    return result
  }
  if (resultColumn.expr() != null) {
    // SELECT expr
    var response = copy(scopedValues = availableValues).resolve(resultColumn.expr()) ?: return emptyList()
    if (resultColumn.column_alias() != null) {
      response = response.copy(
          name = resultColumn.column_alias().text,
          element = resultColumn.column_alias()
      )
    }
    return listOf(response)
  }

  errors.add(ResolutionError.IncompleteRule(resultColumn, "Result set requires at least one column"))
  return emptyList()
}


/**
 * Takes a value rule and returns the columns introduced. Validates that any
 * appended values have the same length.
 */
internal fun Resolver.resolve(values: SqliteParser.ValuesContext): List<Result> {
  val selected = values.expr().map { resolve(it) }.filterNotNull()
  if (values.values() != null) {
    val joinedValues = resolve(values.values())
    if (joinedValues.size != selected.size) {
      errors.add(ResolutionError.ValuesError(values.values(), "Unexpected number of columns in" +
          " values found: ${joinedValues.size} expected: ${selected.size}"))
    }
    // TODO: Type check
  }
  return selected
}
