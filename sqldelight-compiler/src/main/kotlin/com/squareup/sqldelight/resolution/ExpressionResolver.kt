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
  // Check if validation fails, and break early if it does so that expressions are guaranteed
  // well formed before resolution happens.
  val result = Resolution(errors = ExpressionValidator(subqueriesAllowed).validate(expression))
  if (result.errors.isNotEmpty()) return result

  if (expression.column_name() != null) {
    // | ( ( database_name '.' )? table_name '.' )? column_name
    return resolve(scopedValues, expression.column_name(), expression.table_name())
  } else if (expression.literal_value() != null) {
    return resolve(expression.literal_value())
  } else if (expression.BIND_PARAMETER() != null) {
    // TODO: Add something to the resolution saying that a parameter is needed.
    return Resolution()
  } else if (expression.unary_operator() != null) {
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.function_name() != null) {
    return resolveFunction(expression, subqueriesAllowed)
  } else if (expression.K_CAST() != null) {
    val resolution = resolve(expression.expr(0), subqueriesAllowed)
    return Resolution(values = resolution.values.map {
      it.copy(type = Value.SqliteType.valueOf(expression.type_name().sqlite_type_name().text))
    }, errors = resolution.errors)
  } else if (expression.K_COLLATE() != null) {
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.K_ESCAPE() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = (resolve(expression.expr(0), subqueriesAllowed)
            .plus(resolve(expression.expr(1), subqueriesAllowed))
            .plus(resolve(expression.expr(2), subqueriesAllowed))).errors
    )
  } else if (expression.K_ISNULL() != null || expression.K_NOTNULL() != null || expression.K_NOT() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = resolve(expression.expr(0), subqueriesAllowed).errors
    )
  } else if (expression.K_BETWEEN() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = (resolve(expression.expr(0), subqueriesAllowed)
            .plus(resolve(expression.expr(1), subqueriesAllowed))
            .plus(resolve(expression.expr(2), subqueriesAllowed))).errors
    )
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
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = resolution.errors)
  } else if (expression.select_stmt() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = resolve(expression.select_stmt()).errors)
  } else if (expression.K_CASE() != null) {
    val resolutions = expression.expr().map { resolve(it, subqueriesAllowed) }
    return Resolution(
        values = listOf(Value(null, null, resolutions[0].values[0].type, expression, null)),
        errors = resolutions.flatMap { it.errors }
    )
  } else if (expression.OPEN_PAR() != null) {
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.raise_function() != null) {
    return Resolution()
  } else {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, expression, null)),
        errors = expression.expr().foldRight(Resolution(), { expr, resolution ->
          resolution + resolve(expr, subqueriesAllowed)
        }).errors)
  }
}

private fun resolve(literalValue: SqliteParser.Literal_valueContext): Resolution {
  if (literalValue.INTEGER_LITERAL() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.INTEGER, literalValue, null)))
  }
  if (literalValue.REAL_LITERAL() != null) {
    return Resolution(
        values = listOf(Value(null, null, Value.SqliteType.REAL, literalValue, null)))
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

private fun Resolver.resolveFunction(
    expression: SqliteParser.ExprContext,
    subqueriesAllowed: Boolean
): Resolution {
  var sqliteType = Value.SqliteType.NULL
  val resolutions = expression.expr().map { resolve(it, subqueriesAllowed) }
  expression.function_name().text.let { functionName ->
    when (functionName) {
      "date", "time", "datetime", "julianday", "strftime", "char", "hex", "lower", "ltrim",
      "printf", "quote", "replace", "rtrim", "soundex", "sqlite_compileoption_get",
      "sqlite_source_id", "sqlite_version", "substr", "trim", "typeof", "upper",
      "group_concat" -> {
        sqliteType = Value.SqliteType.TEXT
      }
      "changes", "instr", "last_insert_rowid", "length", "random", "sqlite_compileoption_used",
      "total_changes", "unicode", "count" -> {
        sqliteType = Value.SqliteType.INTEGER
      }
      "randomblob", "zeroblob" -> {
        sqliteType = Value.SqliteType.BLOB
      }
      "round", "avg", "sum", "total" -> {
        // TODO: For sum we should check the types for nullability and use INTEGER if possible
        sqliteType = Value.SqliteType.REAL
      }
      "abs", "coalesce", "ifnull", "likelihood", "likely", "nullif", "unlikely" -> {
        // Functions which return the type of their first argument.
        sqliteType = resolutions[0].values[0].type
      }
      "max" -> {
        // NULL < INTEGER < REAL < TEXT < BLOB
        resolutions.map { it.values[0].type }.forEach {
          if (it == Value.SqliteType.BLOB) {
            sqliteType = it
          } else if (it == Value.SqliteType.TEXT && sqliteType != Value.SqliteType.BLOB) {
            sqliteType = it
          } else if (it == Value.SqliteType.REAL && sqliteType != Value.SqliteType.BLOB && sqliteType != Value.SqliteType.TEXT) {
            sqliteType = it
          } else if (it == Value.SqliteType.INTEGER && sqliteType == Value.SqliteType.NULL) {
            sqliteType = it
          }
        }
      }
      "min" -> {
        // BLOB < TEXT < INTEGER < REAL < NULL
        sqliteType = Value.SqliteType.BLOB
        resolutions.map { it.values[0].type }.forEach {
          if (it == Value.SqliteType.NULL) {
            sqliteType = it
          } else if (it == Value.SqliteType.REAL && sqliteType != Value.SqliteType.NULL) {
            sqliteType = it
          } else if (it == Value.SqliteType.INTEGER && sqliteType != Value.SqliteType.NULL && sqliteType != Value.SqliteType.REAL) {
            sqliteType = it
          } else if (it == Value.SqliteType.TEXT && sqliteType == Value.SqliteType.BLOB) {
            sqliteType = it
          }
        }
      }
    }
  }
  return Resolution(
      values = listOf(Value(null, null, sqliteType, expression, null)),
      errors = resolutions.flatMap { it.errors }
  )
}