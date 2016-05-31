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
package com.squareup.sqldelight.validation

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.resolution.resolve
import com.squareup.sqldelight.resolution.ResolutionError
import com.squareup.sqldelight.resolution.ResolutionError.ExpressionError
import com.squareup.sqldelight.types.Value
import org.antlr.v4.runtime.RuleContext

internal open class ExpressionValidator(
    private val resolver: Resolver,
    private val values: List<Value>,
    private val subqueriesAllowed: Boolean = true
) {
  fun validate(expression: SqliteParser.ExprContext): List<ResolutionError> {
    try {
      return validateAndThrow(expression)
    } catch (e: Exception) {
      if (e.message == null) {
        throw e
      } else {
        return listOf(ResolutionError.ExpressionError(expression, e.message!!))
      }
    }
  }

  private fun validateAndThrow(expression: SqliteParser.ExprContext): List<ResolutionError> {
    if (expression.column_name() != null) {
      return resolver.resolve(expression, values).errors
    }
    if (expression.BIND_PARAMETER() != null) {
      // | BIND_PARAMETER
      // This is the android parameter thing. Didnt even know it was here! Neato!
      return emptyList()
    }
    if (expression.literal_value() != null) {
      // : literal_value
      // No validation needed.
      return emptyList()
    }
    if (expression.unary_operator() != null) {
      // | unary_operator expr
      return validate(expression.expr(0))
    }
    if (expression.function_name() != null) {
      // | function_name '(' ( K_DISTINCT? expr ( ',' expr )* | STAR )? ')'
      return expression.validateFunction()
    }
    if (expression.K_CAST() != null) {
      // | K_CAST '(' expr K_AS type_name ')'
      return validate(expression.expr(0))
    }
    if (expression.K_COLLATE() != null) {
      // | expr K_COLLATE collation_name
      return validate(expression.expr(0))
    }
    if (expression.K_LIKE() != null || expression.K_GLOB() != null || expression.K_REGEXP() != null || expression.K_MATCH() != null) {
      // | expr K_NOT? ( K_LIKE | K_GLOB | K_REGEXP | K_MATCH ) expr ( K_ESCAPE expr )?
      val result = validate(expression.expr(0)) + validate(expression.expr(1))
      if (expression.K_ESCAPE() != null) {
        return result + validate(expression.expr(2))
      }
      return result
    }
    if (expression.K_ISNULL() != null || expression.K_NOTNULL() != null || expression.K_NULL() != null) {
      // | expr ( K_ISNULL | K_NOTNULL | K_NOT K_NULL )
      return validate(expression.expr(0))
    }
    if (expression.K_IS() != null) {
      // | expr K_IS K_NOT? expr
      return expression.expr().take(2).flatMap { validate(it) }
    }
    if (expression.K_BETWEEN() != null) {
      // | expr K_NOT? K_BETWEEN expr K_AND expr
      return expression.expr().take(3).flatMap { validate(it) }
    }
    if (expression.K_IN() != null) {
      //  | expr K_NOT? K_IN ( '(' ( select_stmt
      //                           | expr ( ',' expr )*
      //                           )?
      //                       ')'
      //                     | ( database_name '.' )? table_name )
      if (expression.select_stmt() != null) {
        if (!subqueriesAllowed) {
          return listOf(ExpressionError(expression.select_stmt(), "Subqueries are" +
              " not permitted as part of CREATE TABLE statements"))
        }
        return validate(expression.expr(0)) + resolver.copy(scopedValues = values)
            .resolve(expression.select_stmt()).errors
        // TODO checks to make sure this makes sense with the columns returned in the subquery.
      } else if (expression.table_name() != null) {
        // Just make sure the table actually exists by attempting to resolve it.
        return validate(expression.expr(0)) + resolver.resolve(expression.table_name()).errors
      } else {
        return expression.expr().flatMap { validate(it) }
      }
    }
    if (expression.select_stmt() != null) {
      // | ( ( K_NOT )? K_EXISTS )? '(' select_stmt ')'
      if (!subqueriesAllowed) {
        return listOf(ExpressionError(expression.select_stmt(), "Subqueries are" +
            " not permitted as part of CREATE TABLE statements"))
      }
      // We don't do anything with the returned select statement so we can dip.
      return resolver.copy(scopedValues = values).resolve(expression.select_stmt()).errors
    }
    if (expression.K_CASE() != null) {
      // | K_CASE expr? ( K_WHEN expr K_THEN expr )+ ( K_ELSE expr )? K_END
      return expression.expr().flatMap { validate(it) }
    }
    if (expression.raise_function() != null) {
      // No validation needed.
      return emptyList()
    }
    if (expression.OPEN_PAR() != null) {
      // | OPEN_PAR expr CLOSE_PAR
      return validate(expression.expr(0))
    }
    // Binary operator catch all since they use strings and not keywords.
    // | expr binary_operator expr
    // TODO validate the types and operation makes sense.
    return (validate(expression.expr(0)) + validate(expression.expr(1)))
  }

  private fun SqliteParser.ExprContext.validateFunction(): List<ResolutionError> {
    val errors = arrayListOf<ResolutionError>()
    // TODO verify the types of the parameters are correct.

    // Verify the function argument size.
    function_name().text.let {
      when (it) {
        "changes", "last_insert_rowid", "random", "sqlite_source_id", "sqlite_version",
        "total_changes" -> {
          // Takes 0 arguments
          if (expr().size != 0) {
            errors.add(ExpressionError(this, "$it takes no arguments"))
          }
        }
        "count" -> {
          // Takes 1 argument or '*'
          if (expr().size > 1 || (expr().size == 0 && STAR() == null)) {
            errors.add(ExpressionError(this, "$it expects a single argument"))
          }
        }
        "abs", "hex", "length", "likely", "lower", "quote", "randomblob", "soundex",
        "sqlite_compileoption_get", "sqlite_compileoption_used", "typeof", "unlikely", "unicode",
        "upper", "zeroblob", "sum", "total", "avg"  -> {
          // Takes 1 argument.
          if (expr().size != 1) {
            errors.add(ExpressionError(this, "$it expects a single argument"))
          }
        }
        "ifnull", "instr", "likelihood", "nullif" -> {
          // Takes 2 arguments.
          if (expr().size != 2) {
            errors.add(ExpressionError(this, "$it expects two arguments"))
          }
        }
        "replace" -> {
          // Takes 3 arguments
          if (expr().size != 3) {
            errors.add(ExpressionError(this, "$it expects three arguments"))
          }
        }
        "ltrim", "round", "rtrim", "trim" -> {
          // Takes 1 or 2 arguments.
          if (expr().size != 1 && expr().size != 2) {
            errors.add(ExpressionError(this, "$it expects one or two arguments"))
          }
        }
        "substr", "group_concat" -> {
          // Takes 2 or 3 arguments
          if (expr().size != 2 && expr().size != 3) {
            errors.add(ExpressionError(this, "$it expects two or three arguments"))
          }
        }
        "char", "date", "time", "datetime", "juliantime", "max", "min", "printf" -> {
          // Takes at least 1 argument.
          if (expr().size < 1) {
            errors.add(ExpressionError(this, "$it expects 1 or more arguments"))
          }
        }
        "coalesce", "strftime" -> {
          // Takes at least 2 arguments
          if (expr().size < 2) {
            errors.add(ExpressionError(this, "$it expects 2 or more arguments"))
          }
        }
        else -> errors.add(ExpressionError(this, "function $it does not exist"))
      }

      // Verify that aggregate functions aren't misused.
      when (it) {
        "max", "min" -> {
          if (expr().size == 1 && !parent.canContainAggregate()) {
            errors.add(ExpressionError(this, "Aggregate function $it must be" +
                " used in a result column or having clause."))
          }
        }
        "avg", "count", "group_concat", "sum", "total" -> {
          if (!parent.canContainAggregate()) {
            errors.add(ExpressionError(this, "Aggregate function $it must be" +
                " used in a result column or having clause."))
          }
        }
        else -> {
          // Non aggregate functions cannot use 'DISTINCT'
          if (K_DISTINCT() != null) {
            errors.add(ExpressionError(this, "Non aggregate functions cannot use DISTINCT"))
          }
        }
      }
    }
    errors.addAll(expr().flatMap { validate(it) })
    return errors
  }

  private fun RuleContext.canContainAggregate(): Boolean = this is SqliteParser.Result_columnContext
      || this is SqliteParser.Having_stmtContext || (parent != null && parent.canContainAggregate())
}
