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
import com.squareup.sqldelight.types.Value
import com.squareup.sqldelight.types.columns
import com.squareup.sqldelight.validation.ExpressionValidator

/**
 * Takes a list of available values and returns a selected value.
 */
internal fun Resolver.resolve(expression: SqliteParser.ExprContext, availableValues: List<Value>): Resolution {
  if (expression.column_name() != null) {
    // | ( ( database_name '.' )? table_name '.' )? column_name
    val matchingColumns = availableValues.columns(expression.column_name().text,
        expression.table_name()?.text)
    if (matchingColumns.isEmpty()) {
      return Resolution(ResolutionError.ColumnOrTableNameNotFound(
          expression,
          "No column found with name ${expression.column_name().text}",
          availableValues,
          expression.table_name()?.text
      ))
    } else if (matchingColumns.size > 1) {
      return Resolution(ResolutionError.ExpressionError(
          expression,
          "Ambiguous column name ${expression.column_name().text}, " +
              "found in tables ${matchingColumns.map { it.tableName }}"
      ))
    } else {
      return Resolution(matchingColumns)
          .findElement(expression.table_name(), matchingColumns.first().tableNameElement, elementToFind)
          .findElement(expression.column_name(), matchingColumns.first().element, elementToFind)
    }
  }

  // TODO get the actual type of the expression. Thats gonna be fun. :(
  return Resolution(
      values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
      errors = ExpressionValidator(this, availableValues).validate(expression))
}