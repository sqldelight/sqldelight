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
import com.squareup.sqldelight.validation.ExpressionValidator

internal fun Resolver.resolve(
    expression: SqliteParser.ExprContext,
    subqueriesAllowed: Boolean = true
): Resolution {
  var result = Resolution(errors = ExpressionValidator(subqueriesAllowed).validate(expression))

  if (expression.column_name() != null) {
    // | ( ( database_name '.' )? table_name '.' )? column_name
    result += resolve(scopedValues, expression.column_name(), expression.table_name())
  } else if (expression.literal_value() != null) {
    result += resolve(expression.literal_value())
  } else if (expression.BIND_PARAMETER() != null) {
    // TODO: Add something to the resolution saying that a parameter is needed.
  } else if (expression.unary_operator() != null) {
    result += resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.function_name() != null) {
    val resolution = expression.expr().foldRight(Resolution(), { expr, resolution ->
      resolution + resolve(expr, subqueriesAllowed)
    })
    result += Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = resolution.errors
    )
  } else if (expression.K_CAST() != null) {
    val resolution = resolve(expression.expr(0), subqueriesAllowed)
    result += Resolution(values = resolution.values.map {
      it.copy(type = Value.SqliteType.valueOf(expression.type_name().sqlite_type_name().text))
    }, errors = resolution.errors)
  } else if (expression.K_COLLATE() != null) {
    result += resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.K_ESCAPE() != null) {
    result += resolve(expression.expr(0), subqueriesAllowed)
        .plus(resolve(expression.expr(1), subqueriesAllowed))
        .plus(resolve(expression.expr(2), subqueriesAllowed))
  } else if (expression.K_ISNULL() != null || expression.K_NOTNULL() != null || expression.K_NOT() != null) {
    result += resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.K_BETWEEN() != null) {
    result += resolve(expression.expr(0), subqueriesAllowed)
        .plus(resolve(expression.expr(1), subqueriesAllowed))
        .plus(resolve(expression.expr(2), subqueriesAllowed))
  } else if (expression.K_IN() != null) {
    var resolution = resolve(expression.expr(0), subqueriesAllowed)
    if (expression.select_stmt() != null) {
      resolution += resolve(expression.select_stmt())
    } else if (expression.table_name() != null) {
      resolution += resolve(expression.table_name())
    } else {
      resolution += expression.expr().drop(1).foldRight(Resolution(), { expr, resolution ->
        resolution + resolve(expr, subqueriesAllowed)
      })
    }
    result += Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = resolution.errors)
  } else if (expression.select_stmt() != null) {
    result += Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = resolve(expression.select_stmt()).errors)
  } else if (expression.K_CASE() != null) {
    result += expression.expr().foldRight(Resolution(), { expr, resolution ->
      resolution + resolve(expr, subqueriesAllowed)
    })
  } else if (expression.OPEN_PAR() != null) {
    result += resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.raise_function() != null) {
    result += Resolution()
  } else {
    result += Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = expression.expr().foldRight(Resolution(), { expr, resolution ->
          resolution + resolve(expr, subqueriesAllowed)
        }).errors)
  }

  return result
}

private fun resolve(literalValue: SqliteParser.Literal_valueContext): Resolution {
  if (literalValue.NUMERIC_LITERAL() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, literalValue, null)))
  }
  if (literalValue.STRING_LITERAL() != null) {
    return Resolution(values = listOf(Value(null, null, Value.SqliteType.TEXT, literalValue, null)))
  }
  if (literalValue.BLOB_LITERAL() != null) {
    return Resolution(values = listOf(Value(null, null, Value.SqliteType.BLOB, literalValue, null)))
  }
  if (literalValue.K_NULL() != null) {
    return Resolution(values = listOf(Value(null, null, Value.SqliteType.NULL, literalValue, null)))
  }
  return Resolution(ResolutionError.IncompleteRule(literalValue, "Unsupported literal"))
}