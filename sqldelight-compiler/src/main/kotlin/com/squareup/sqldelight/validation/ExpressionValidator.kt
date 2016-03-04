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
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.types.Resolver
import com.squareup.sqldelight.types.Value
import com.squareup.sqldelight.types.columns

internal open class ExpressionValidator(
    private val resolver: Resolver,
    private val values: List<Value>
) {
  fun validate(expression: SqliteParser.ExprContext) {
    if (expression.column_name() != null) {
      // | ( ( database_name '.' )? table_name '.' )? column_name
      val matchingColumns = values.columns(expression.column_name().text,
          expression.table_name()?.text)
      if (matchingColumns.isEmpty()) {
        throw SqlitePluginException(expression,
            "No column found with name ${expression.column_name().text}")
      }
      if (matchingColumns.size > 1) {
        throw SqlitePluginException(expression,
            "Ambiguous column name ${expression.column_name().text}, " +
                "founds in tables ${matchingColumns.map { it.tableName }}")
      }
      return
    }
    if (expression.BIND_PARAMETER() != null) {
      // | BIND_PARAMETER
      // This is the android parameter thing. Didnt even know it was here! Neato!
      return
    }
    if (expression.literal_value() != null) {
      // : literal_value
      // No validation needed.
      return
    }
    if (expression.unary_operator() != null) {
      // | unary_operator expr
      validate(expression.expr(0))
      return
    }
    if (expression.binary_operator() != null) {
      // | expr binary_operator expr
      // TODO validate the types and operation makes sense.
      validate(expression.expr(0))
      validate(expression.expr(1))
      return
    }
    if (expression.function_name() != null) {
      // | function_name '(' ( K_DISTINCT? expr ( ',' expr )* | '*' )? ')'
      // TODO validate the function name exists and is valid for the expression type.
      expression.expr().forEach { validate(it) }
      return
    }
    if (expression.K_CAST() != null) {
      // | K_CAST '(' expr K_AS type_name ')'
      validate(expression.expr(0))
      return
    }
    if (expression.K_COLLATE() != null) {
      // | expr K_COLLATE collation_name
      validate(expression.expr(0))
      return
    }
    if (expression.K_LIKE() != null || expression.K_GLOB() != null || expression.K_REGEXP() != null || expression.K_MATCH() != null) {
      // | expr K_NOT? ( K_LIKE | K_GLOB | K_REGEXP | K_MATCH ) expr ( K_ESCAPE expr )?
      validate(expression.expr(0))
      validate(expression.expr(1))
      if (expression.K_ESCAPE() != null) {
        validate(expression.expr(2))
      }
      return
    }
    if (expression.K_ISNULL() != null || expression.K_NOTNULL() != null || expression.K_NULL() != null) {
      // | expr ( K_ISNULL | K_NOTNULL | K_NOT K_NULL )
      validate(expression.expr(0))
      return
    }
    if (expression.K_IS() != null) {
      // | expr K_IS K_NOT? expr
      validate(expression.expr(0))
      validate(expression.expr(1))
      return
    }
    if (expression.K_BETWEEN() != null) {
      // | expr K_NOT? K_BETWEEN expr K_AND expr
      validate(expression.expr(0))
      validate(expression.expr(1))
      validate(expression.expr(2))
      return
    }
    if (expression.K_IN() != null) {
      //  | expr K_NOT? K_IN ( '(' ( select_stmt
      //                           | expr ( ',' expr )*
      //                           )?
      //                       ')'
      //                     | ( database_name '.' )? table_name )
      validate(expression.expr(0))
      if (expression.select_stmt() != null) {
        SelectStmtValidator(resolver, values).validate(expression.select_stmt())
      } else if (expression.table_name() != null) {
        // Just make sure the table actually exists by attempting to resolve it.
        resolver.resolve(expression.table_name())
      } else {
        expression.expr().drop(1).forEach { validate(it) }
      }
      return
    }
    if (expression.select_stmt() != null) {
      // | ( ( K_NOT )? K_EXISTS )? '(' select_stmt ')'
      SelectStmtValidator(resolver, values).validate(expression.select_stmt())
      return
    }
    if (expression.K_CASE() != null) {
      // | K_CASE expr? ( K_WHEN expr K_THEN expr )+ ( K_ELSE expr )? K_END
      expression.expr().forEach { validate(it) }
      return
    }
    if (expression.raise_function() != null) {
      // No validation needed.
      return
    }
  }

}
