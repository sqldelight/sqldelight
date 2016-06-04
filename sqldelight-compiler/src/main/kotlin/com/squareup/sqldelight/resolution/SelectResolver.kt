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
import com.squareup.sqldelight.types.Value
import com.squareup.sqldelight.validation.SelectOrValuesValidator
import com.squareup.sqldelight.validation.SelectStmtValidator

internal fun Resolver.resolve(selectStmt: SqliteParser.Select_stmtContext): Resolution {
  val resolver = if (selectStmt.with_clause() != null) {
    try {
      withResolver(selectStmt.with_clause())
    } catch (e: SqlitePluginException) {
      return Resolution(ResolutionError.WithTableError(e.originatingElement, e.message))
    }
  } else {
    this
  }

  var resolution = resolver.resolve(selectStmt.select_or_values(0), selectStmt)

  // Resolve other compound select statements and verify they have equivalent columns.
  selectStmt.select_or_values().drop(1).forEach {
    val compoundValues = resolver.resolve(it)
    if (compoundValues.values.size != resolution.values.size) {
      resolution += Resolution(ResolutionError.CompoundError(it,
          "Unexpected number of columns in compound statement found: " +
              "${compoundValues.values.size} expected: ${resolution.values.size}"))
    }
    resolution += compoundValues.copy(values = emptyList())
    // TODO: Type checking.
    //for (valueIndex in 0..values.size) {
    //  if (values[valueIndex].type != compoundValues[valueIndex].type) {
    //    throw SqlitePluginException(compoundValues[valueIndex].element, "Incompatible types in " +
    //        "compound statement for column 2 found: ${compoundValues[valueIndex].type} " +
    //        "expected: ${values[valueIndex].type}")
    //  }
    //}
  }

  return resolution
}

/**
 * Takes a select_or_values rule and returns the columns selected.
 */
internal fun Resolver.resolve(
    selectOrValues: SqliteParser.Select_or_valuesContext,
    parentSelect: SqliteParser.Select_stmtContext? = null
): Resolution {
  var resolution: Resolution
  if (selectOrValues.K_VALUES() != null) {
    // No columns are available, only selected columns are returned.
    return Resolution(errors = SelectOrValuesValidator(this, scopedValues)
            .validate(selectOrValues)) + resolve(selectOrValues.values())
  } else if (selectOrValues.join_clause() != null) {
    resolution = resolve(selectOrValues.join_clause())
  } else if (selectOrValues.table_or_subquery().size > 0) {
    resolution = selectOrValues.table_or_subquery().fold(Resolution()) {
      response, table_or_subquery -> response + resolve(table_or_subquery)
    }
  } else {
    resolution = Resolution()
  }

  // Validate the select or values has valid expressions before aliasing/selection.
  resolution += Resolution(
      errors = SelectOrValuesValidator(this, scopedValues + resolution.values)
          .validate(selectOrValues))

  if (parentSelect != null) {
    resolution += Resolution(
        errors = SelectStmtValidator(this, scopedValues + resolution.values)
            .validate(parentSelect))
  }

  return selectOrValues.result_column().fold(Resolution(errors = resolution.errors)) {
    response, result_column -> response + resolve(result_column, resolution.values)
  }
}

/**
 * Take in a list of available columns and return a list of selected columns.
 */
internal fun Resolver.resolve(
    resultColumn: SqliteParser.Result_columnContext,
    availableValues: List<Value>
): Resolution {
  if (resultColumn.text.equals("*")) {
    // SELECT *
    return Resolution(availableValues)
  }
  if (resultColumn.table_name() != null) {
    // SELECT some_table.*
    val result = Resolution(availableValues.filter {
      it.tableName == resultColumn.table_name().text
    })
    if (result.values.isEmpty()) {
      return Resolution(errors = listOf(ResolutionError.TableNameNotFound(
              resultColumn.table_name(),
              "Table name ${resultColumn.table_name().text} not found",
              availableValues.map { it.tableName }.filterNotNull().distinct())))
    }
    return result.findElement(resultColumn.table_name(), result.values.first().tableNameElement!!, elementToFind)
  }
  if (resultColumn.expr() != null) {
    // SELECT expr
    var response = copy(scopedValues = availableValues).resolve(resultColumn.expr())
    if (resultColumn.column_alias() != null) {
      response = Resolution(response.values.map {
        it.copy(columnName = resultColumn.column_alias().text,
            element = resultColumn.column_alias())
      }, response.errors)
    }
    return response
  }

  return Resolution(
      ResolutionError.IncompleteRule(resultColumn, "Result set requires at least one column"))
}


/**
 * Takes a value rule and returns the columns introduced. Validates that any
 * appended values have the same length.
 */
internal fun Resolver.resolve(values: SqliteParser.ValuesContext): Resolution {
  var selected = values.expr().fold(Resolution(), { response, expression ->
    response + resolve(expression)
  })
  if (values.values() != null) {
    val joinedValues = resolve(values.values())
    selected += Resolution(errors = joinedValues.errors)
    if (joinedValues.values.size != selected.values.size) {
      selected += Resolution(ResolutionError.ValuesError(values.values(),
          "Unexpected number of columns in values found: ${joinedValues.values.size} " +
              "expected: ${selected.values.size}"))
    }
    // TODO: Type check
  }
  return selected
}