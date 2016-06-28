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
import com.squareup.sqldelight.resolution.query.Value
import com.squareup.sqldelight.resolution.query.ceilValue
import com.squareup.sqldelight.resolution.query.resultColumnSize
import com.squareup.sqldelight.types.SqliteType
import com.squareup.sqldelight.validation.ExpressionValidator

internal fun Resolver.resolve(
    expression: SqliteParser.ExprContext,
    subqueriesAllowed: Boolean = true
): Value? {
  // Check if validation fails, and break early if it does so that expressions are guaranteed
  // well formed before resolution happens.
  val exprErrors = ExpressionValidator(subqueriesAllowed).validate(expression)
  errors.addAll(exprErrors)
  if (exprErrors.isNotEmpty()) return null

  if (expression.column_name() != null) {
    // | ( ( database_name '.' )? table_name '.' )? column_name
    return resolve(scopedValues, expression.column_name(), expression.table_name()) as? Value
  } else if (expression.literal_value() != null) {
    try {
      return expression.literal_value().resolve(expression)
    } catch (e: SqlitePluginException) {
      errors.add(ResolutionError.IncompleteRule(e.originatingElement, e.message))
      return null
    }
  } else if (expression.BIND_PARAMETER() != null) {
    // TODO: Add something to the resolution saying that a parameter is needed.
    return null
  } else if (expression.unary_operator() != null) {
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.function_name() != null) {
    return resolveFunction(expression, subqueriesAllowed)
  } else if (expression.K_CAST() != null) {
    val type = SqliteType.valueOf(expression.type_name().sqlite_type_name().text).defaultType
    return resolve(expression.expr(0), subqueriesAllowed)?.copy(javaType = type, dataType = type)
  } else if (expression.K_COLLATE() != null) {
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.K_ESCAPE() != null) {
    resolve(expression.expr(0), subqueriesAllowed)
    resolve(expression.expr(1), subqueriesAllowed)
    resolve(expression.expr(2), subqueriesAllowed)
    return Value(expression, SqliteType.INTEGER.defaultType, false)
  } else if (expression.K_ISNULL() != null || expression.K_NOTNULL() != null || expression.K_NOT() != null) {
    resolve(expression.expr(0))
    return Value(expression, SqliteType.INTEGER.defaultType, false)
  } else if (expression.K_BETWEEN() != null) {
    resolve(expression.expr(0), subqueriesAllowed)
    resolve(expression.expr(1), subqueriesAllowed)
    resolve(expression.expr(2), subqueriesAllowed)
    return Value(expression, SqliteType.INTEGER.defaultType, false)
  } else if (expression.K_IN() != null) {
    resolve(expression.expr(0), subqueriesAllowed)
    if (expression.select_stmt() != null) {
      val selectResolution = resolve(expression.select_stmt())
      if (selectResolution.resultColumnSize() > 1) {
        errors.add(ResolutionError.ExpressionError(expression.select_stmt(),
            "Subquerys used for IN can only return a single result column."))
      }
    } else if (expression.table_name() != null) {
      Resolver(symbolTable, dependencies, errors = errors, elementFound = elementFound)
          .resolve(expression.table_name())
    } else {
      expression.expr().drop(1).forEach { resolve(it, subqueriesAllowed) }
    }
    return Value(expression, SqliteType.INTEGER.defaultType, false)
  } else if (expression.select_stmt() != null) {
    resolve(expression.select_stmt())
    return Value(expression, SqliteType.INTEGER.defaultType, false)
  } else if (expression.K_CASE() != null) {
    expression.expr().forEach { resolve(it, subqueriesAllowed) }
    return expression.return_expr().map { resolve(it.expr(), subqueriesAllowed) }.filterNotNull().ceilValue(expression)
  } else if (expression.OPEN_PAR() != null) {
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.raise_function() != null) {
    return null
  } else {
    return expression.expr().map { resolve(it, subqueriesAllowed) }.filterNotNull().ceilValue(expression)
  }
}

private fun SqliteParser.Literal_valueContext.resolve(expression: SqliteParser.ExprContext): Value {
  if (INTEGER_LITERAL() != null) {
    return Value(expression, SqliteType.INTEGER.defaultType, false)
  }
  if (REAL_LITERAL() != null) {
    return Value(expression, SqliteType.REAL.defaultType, false)
  }
  if (STRING_LITERAL() != null) {
    return Value(expression, SqliteType.TEXT.defaultType, false)
  }
  if (BLOB_LITERAL() != null) {
    return Value(expression, SqliteType.BLOB.defaultType, false)
  }
  if (K_NULL() != null) {
    return Value(expression, SqliteType.NULL.defaultType, true)
  }
  throw SqlitePluginException(this, "Unsupported literal")
}

private fun Resolver.resolveFunction(
    expression: SqliteParser.ExprContext,
    subqueriesAllowed: Boolean
): Value? {
  val resolutions = expression.expr().map { resolve(it, subqueriesAllowed) }
  expression.function_name().text.toLowerCase().let { functionName ->
    when (functionName) {
      "date", "time", "datetime", "julianday", "strftime", "char", "hex", "quote", "soundex",
      "sqlite_compileoption_get", "sqlite_source_id", "sqlite_version", "typeof", "upper",
      "group_concat" -> {
        // Functions that return a non-null string.
        return Value(expression, SqliteType.TEXT.defaultType, false)
      }
      "lower", "ltrim", "printf", "replace", "rtrim", "substr", "trim", "upper" -> {
        // Functions that take return a string with the nullability of their first parameter.
        return Value(expression, SqliteType.TEXT.defaultType, resolutions.first()?.nullable ?: false)
      }
      "changes", "last_insert_rowid", "random", "sqlite_compileoption_used", "total_changes",
      "count" -> {
        // Functions that return a non-null integer.
        return Value(expression, SqliteType.INTEGER.defaultType, false)
      }
      "instr", "length", "unicode" -> {
        // Functions that return an integer that is nullability if any of its parameters are
        // nullable.
        return Value(expression, SqliteType.INTEGER.defaultType,
            resolutions.filterNotNull().any { it.nullable })
      }
      "randomblob", "zeroblob" -> {
        return Value(expression, SqliteType.BLOB.defaultType, false)
      }
      "total" -> {
        // Returns a non-null real.
        return Value(expression, SqliteType.REAL.defaultType, false)
      }
      "sum", "round", "sum" -> {
        // Returns a real with the nullability of its first argument.
        return Value(expression, SqliteType.REAL.defaultType, resolutions.first()?.nullable ?: false)
      }
      "abs", "likelihood", "likely", "nullif", "unlikely" -> {
        // Functions which return the type of their first argument.
        val argument = resolutions.first()
        return Value(expression, argument?.dataType ?: SqliteType.NULL.defaultType,
            argument?.nullable ?: false)
      }
      "coalesce", "ifnull" -> {
        // Functions that return their first non-null argument.
        return Value(
            expression,
            resolutions.filterNotNull().ceilValue(expression).dataType,
            !resolutions.filterNotNull().any { !it.nullable }
        )
      }
      "nullif" -> {
        // Returns first argument, or null if they are the same (always nullable).
        return Value(expression, resolutions.first()?.dataType ?: SqliteType.NULL.defaultType, true)
      }
      "max" -> {
        // NULL < INTEGER < REAL < TEXT < BLOB
        var sqliteType = SqliteType.NULL
        if (resolutions.filterNotNull().resultColumnSize() == 0) return null
        resolutions.filterNotNull().forEach {
          if (it.dataType == SqliteType.BLOB.defaultType) {
            sqliteType = SqliteType.BLOB
          } else if (SqliteType.TEXT.contains(it.dataType) && sqliteType != SqliteType.BLOB) {
            sqliteType = SqliteType.TEXT
          } else if (SqliteType.REAL.contains(it.dataType) && sqliteType != SqliteType.BLOB && sqliteType != SqliteType.TEXT) {
            sqliteType = SqliteType.REAL
          } else if (SqliteType.INTEGER.contains(it.dataType) && sqliteType == SqliteType.NULL) {
            sqliteType = SqliteType.INTEGER
          }
        }
        return Value(expression, sqliteType.defaultType, true)
      }
      "min" -> {
        // BLOB < TEXT < INTEGER < REAL < NULL
        var sqliteType = SqliteType.BLOB
        if (resolutions.filterNotNull().resultColumnSize() == 0) return null
        resolutions.filterNotNull().forEach {
          if (SqliteType.NULL.contains(it.dataType)) {
            sqliteType = SqliteType.NULL
          } else if (SqliteType.REAL.contains(it.dataType) && sqliteType != SqliteType.NULL) {
            sqliteType = SqliteType.REAL
          } else if (SqliteType.INTEGER.contains(it.dataType) && sqliteType != SqliteType.NULL && sqliteType != SqliteType.REAL) {
            sqliteType = SqliteType.INTEGER
          } else if (SqliteType.TEXT.contains(it.dataType) && sqliteType == SqliteType.BLOB) {
            sqliteType = SqliteType.REAL
          }
        }
        return Value(expression, sqliteType.defaultType, true)
      }
      else -> {
        return null
      }
    }
  }
}
