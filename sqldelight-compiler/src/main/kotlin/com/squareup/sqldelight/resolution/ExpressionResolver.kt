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
import com.squareup.sqldelight.types.Argument
import com.squareup.sqldelight.types.ArgumentType
import com.squareup.sqldelight.types.ArgumentType.SingleValue
import com.squareup.sqldelight.types.SqliteType
import com.squareup.sqldelight.types.SqliteType.BLOB
import com.squareup.sqldelight.types.SqliteType.INTEGER
import com.squareup.sqldelight.types.SqliteType.REAL
import com.squareup.sqldelight.types.SqliteType.TEXT
import com.squareup.sqldelight.validation.ExpressionValidator

internal fun Resolver.resolve(
    expression: SqliteParser.ExprContext,
    subqueriesAllowed: Boolean = true,
    expectedType: ArgumentType = SingleValue(null)
): Value? {
  // Check if validation fails, and break early if it does so that expressions are guaranteed
  // well formed before resolution happens.
  val exprErrors = ExpressionValidator(subqueriesAllowed).validate(expression)
  errors.addAll(exprErrors)
  if (exprErrors.isNotEmpty()) return null

  if (expression.column_name() != null) {
    // | ( ( database_name '.' )? table_name '.' )? column_name
    return scopedResolve(scopedValues, expression.column_name(), expression.table_name()) as? Value
  } else if (expression.literal_value() != null) {
    // : literal_value
    try {
      return expression.literal_value().resolve(expression)
    } catch (e: SqlitePluginException) {
      errors.add(ResolutionError.IncompleteRule(e.originatingElement, e.message))
      return null
    }
  } else if (expression.K_IN() != null) {
    // | expr K_NOT? K_IN ( '(' ( select_stmt
    //                          | expr ( ',' expr )*
    //                          )?
    //                      ')'
    //                    | table_name
    //                    | ( BIND_DIGITS | ( ':' ) IDENTIFIER ) )
    val comparator = resolve(expression.expr(0), subqueriesAllowed)
    if (expression.select_stmt() != null) {
      val selectResolution = resolve(expression.select_stmt())
      if (selectResolution.resultColumnSize() > 1) {
        errors.add(ResolutionError.ExpressionError(expression.select_stmt(),
            "Subquerys used for IN can only return a single result column."))
      }
    } else if (expression.table_name() != null) {
      Resolver(symbolTable, dependencies, errors = errors, elementFound = elementFound, arguments = arguments)
          .resolve(expression.table_name())
    } else if (expression.BIND_DIGITS() != null || expression.IDENTIFIER() != null) {
      val start = expression.BIND_DIGITS()?.symbol?.startIndex ?: expression.IDENTIFIER().symbol.startIndex - 1
      val stop = expression.BIND_DIGITS()?.symbol?.stopIndex ?: expression.IDENTIFIER().symbol.stopIndex
      arguments.add(Argument(
          argumentType = ArgumentType.SetOfValues(comparator),
          ranges = arrayListOf(IntRange(start, stop)),
          index = expression.BIND_DIGITS()?.text?.substring(1)?.let { if (it.isEmpty()) null else it.toInt() },
          name = expression.IDENTIFIER()?.text
      ))
    } else {
      expression.expr().drop(1).forEach { resolve(it, subqueriesAllowed, SingleValue(comparator)) }
    }
    return Value(expression, INTEGER, false)
  } else if (expression.BIND_DIGITS() != null || expression.IDENTIFIER() != null) {
    // | ( BIND_DIGITS | ( ':' ) IDENTIFIER )
    arguments.add(Argument(
        argumentType = expectedType,
        ranges = arrayListOf(IntRange(expression.start.startIndex, expression.stop.stopIndex)),
        index = expression.BIND_DIGITS()?.text?.substring(1)?.let { if (it.isEmpty()) null else it.toInt() },
        name = expression.IDENTIFIER()?.text
    ))
    return expectedType.comparable
  } else if (expression.unary_operator() != null) {
    // | unary_operator expr
    return resolve(expression.expr(0), subqueriesAllowed, expectedType)
  } else if (expression.function_name() != null) {
    // | function_name '(' ( K_DISTINCT? expr ( ',' expr )* | STAR )? ')'
    return resolveFunction(expression, subqueriesAllowed)
  } else if (expression.K_CAST() != null) {
    // | K_CAST '(' expr K_AS type_name ')'
    val type = SqliteType.valueOf(expression.type_name().sqlite_type_name().text)
    return resolve(expression.expr(0), subqueriesAllowed)?.copy(javaType = type.defaultType, dataType = type)
  } else if (expression.K_COLLATE() != null) {
    // | expr K_COLLATE collation_name
    return resolve(expression.expr(0), subqueriesAllowed)
  } else if (expression.K_ISNULL() != null || expression.K_NOTNULL() != null || expression.K_NULL() != null) {
    // | expr ( K_ISNULL | K_NOTNULL | K_NOT K_NULL )
    resolve(expression.expr(0))
    return Value(expression, INTEGER, false)
  } else if (expression.K_LIKE() != null || expression.K_GLOB() != null
      || expression.K_REGEXP() != null || expression.K_MATCH() != null
      || expression.K_IS() != null || expression.EQ() != null || expression.ASSIGN() != null
      || expression.NOT_EQ1() != null || expression.NOT_EQ2() != null || expression.LT() != null
      || expression.LT_EQ() != null || expression.GT() != null || expression.GT_EQ() != null) {
    // | expr K_NOT? ( K_LIKE | K_GLOB | K_REGEXP | K_MATCH ) expr ( K_ESCAPE expr )?
    // | expr K_IS K_NOT? expr
    // | expr ( ASSIGN | EQ | NOT_EQ1 | NOT_EQ2 ) expr
    // | expr ( LT | LT_EQ | GT | GT_EQ ) expr
    val leftType = resolve(expression.expr(0), subqueriesAllowed)
    resolve(expression.expr(1), subqueriesAllowed, expectedType = SingleValue(leftType))
    if (expression.K_ESCAPE() != null) {
      resolve(expression.expr(2), subqueriesAllowed,
          expectedType = SingleValue(Value(expression.expr(2), TEXT, false)))
    }
    return Value(expression, INTEGER, false)
  } else if (expression.K_BETWEEN() != null) {
    // | expr K_NOT? K_BETWEEN expr K_AND expr
    val comparator = resolve(expression.expr(0), subqueriesAllowed)
    resolve(expression.expr(1), subqueriesAllowed, SingleValue(comparator))
    resolve(expression.expr(2), subqueriesAllowed, SingleValue(comparator))
    return Value(expression, INTEGER, false)
  } else if (expression.select_stmt() != null) {
    resolve(expression.select_stmt())
    // | ( ( K_NOT )? K_EXISTS )? '(' select_stmt ')'
    return Value(expression, INTEGER, false)
  } else if (expression.K_CASE() != null) {
    // | K_CASE expr? ( K_WHEN expr K_THEN return_expr )+ ( K_ELSE expr )? K_END
    expression.expr().forEach { resolve(it, subqueriesAllowed) }
    return expression.return_expr().map { resolve(it.expr(), subqueriesAllowed) }.filterNotNull().ceilValue(expression)
  } else if (expression.OPEN_PAR() != null) {
    // | OPEN_PAR expr CLOSE_PAR
    return resolve(expression.expr(0), subqueriesAllowed, expectedType)
  } else if (expression.raise_function() != null) {
    // | raise_function
    return null
  } else if (expression.PIPE2() != null) {
    // | expr PIPE2 expr
    val left = resolve(expression.expr(0), subqueriesAllowed, SingleValue(Value(expression, TEXT, true)))
    val right = resolve(expression.expr(1), subqueriesAllowed, SingleValue(Value(expression, TEXT, true)))
    return Value(expression, TEXT, left?.nullable ?: true || right?.nullable ?: true)
  } else if (expression.DIV() != null || expression.MOD() != null) {
    // | expr ( DIV | MOD ) expr
    resolve(expression.expr(0), subqueriesAllowed, SingleValue(Value(expression, REAL, false)))
    resolve(expression.expr(1), subqueriesAllowed, SingleValue(Value(expression, REAL, false)))
    return Value(expression, REAL, false)
  } else if (expression.STAR() != null || expression.MOD() != null || expression.PLUS() != null
      || expression.MINUS() != null) {
    // | expr ( STAR ) expr
    // | expr ( PLUS | MINUS ) expr
    return expression.expr()
        .map { resolve(it, subqueriesAllowed, SingleValue(Value(expression, REAL, false))) }
        .filterNotNull()
        .ceilValue(expression)
  } else if (expression.LT2() != null || expression.GT2() != null || expression.AMP() != null
      || expression.PIPE() != null) {
    // | expr ( LT2 | GT2 | AMP | PIPE ) expr
    resolve(expression.expr(0), subqueriesAllowed)
    resolve(expression.expr(1), subqueriesAllowed)
    return Value(expression, BLOB, false)
  } else if (expression.K_AND() != null || expression.K_OR() != null) {
    resolve(expression.expr(0), subqueriesAllowed, ArgumentType.boolean(expression))
    resolve(expression.expr(1), subqueriesAllowed, ArgumentType.boolean(expression))
    return Value(expression, INTEGER, false)
  }
  throw IllegalStateException("Unexpected expression ${expression.text}")
}

private fun SqliteParser.Literal_valueContext.resolve(expression: SqliteParser.ExprContext): Value {
  if (INTEGER_LITERAL() != null) {
    return Value(expression, INTEGER, false)
  }
  if (REAL_LITERAL() != null) {
    return Value(expression, REAL, false)
  }
  if (STRING_LITERAL() != null) {
    return Value(expression, TEXT, false)
  }
  if (BLOB_LITERAL() != null) {
    return Value(expression, BLOB, false)
  }
  if (K_NULL() != null) {
    return Value(expression, SqliteType.NULL, true)
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
      "sqlite_compileoption_get", "sqlite_source_id", "sqlite_version", "typeof" -> {
        // Functions that return a non-null string.
        return Value(expression, TEXT, false)
      }
      "lower", "ltrim", "printf", "replace", "rtrim", "substr", "trim", "upper", "group_concat" -> {
        // Functions that return a string with the nullability of their first parameter.
        return Value(expression, TEXT, resolutions.first()?.nullable ?: false)
      }
      "changes", "last_insert_rowid", "random", "sqlite_compileoption_used", "total_changes",
      "count" -> {
        // Functions that return a non-null integer.
        return Value(expression, INTEGER, false)
      }
      "instr", "length", "unicode" -> {
        // Functions that return an integer that is nullability if any of its parameters are
        // nullable.
        return Value(expression, INTEGER, resolutions.filterNotNull().any { it.nullable })
      }
      "randomblob", "zeroblob" -> {
        return Value(expression, BLOB, false)
      }
      "total" -> {
        // Returns a non-null real.
        return Value(expression, REAL, false)
      }
      "round" -> {
        // Single arg round function returns an int. Otherwise real.
        if (resolutions.size == 1) {
          return Value(expression, INTEGER, resolutions.first()?.nullable ?: false)
        }
        return Value(expression, REAL, resolutions.first()?.nullable ?: false)
      }
      "sum" -> {
        // If the result column is an integer, sum returns an integer.
        if (INTEGER.contains(resolutions.first()!!.javaType)) {
          return Value(expression, INTEGER, resolutions.first()?.nullable ?: false)
        }
        return Value(expression, REAL, resolutions.first()?.nullable ?: false)
      }
      "abs", "likelihood", "likely", "unlikely" -> {
        // Functions which return the type of their first argument.
        val argument = resolutions.first()
        return Value(expression, argument?.dataType ?: SqliteType.NULL, argument?.nullable ?: false)
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
        return Value(expression, resolutions.first()?.dataType ?: SqliteType.NULL, true)
      }
      "max" -> {
        // NULL < INTEGER < REAL < TEXT < BLOB
        var sqliteType = SqliteType.NULL
        if (resolutions.filterNotNull().resultColumnSize() == 0) return null
        resolutions.filterNotNull().forEach {
          if (it.dataType == BLOB) {
            sqliteType = BLOB
          } else if (TEXT == it.dataType && sqliteType != BLOB) {
            sqliteType = TEXT
          } else if (REAL == it.dataType && sqliteType != BLOB && sqliteType != TEXT) {
            sqliteType = REAL
          } else if (INTEGER == it.dataType && sqliteType == SqliteType.NULL) {
            sqliteType = INTEGER
          }
        }
        return Value(expression, sqliteType, true)
      }
      "min" -> {
        // BLOB < TEXT < INTEGER < REAL < NULL
        var sqliteType = BLOB
        if (resolutions.filterNotNull().resultColumnSize() == 0) return null
        resolutions.filterNotNull().forEach {
          if (SqliteType.NULL == it.dataType) {
            sqliteType = SqliteType.NULL
          } else if (REAL == it.dataType && sqliteType != SqliteType.NULL) {
            sqliteType = REAL
          } else if (INTEGER == it.dataType && sqliteType != SqliteType.NULL && sqliteType != REAL) {
            sqliteType = INTEGER
          } else if (TEXT == it.dataType && sqliteType == BLOB) {
            sqliteType = REAL
          }
        }
        return Value(expression, sqliteType, true)
      }
      else -> {
        return null
      }
    }
  }
}
