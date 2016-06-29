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

internal class SelectOrValuesValidator(private val resolver: Resolver) {
  fun validate(selectOrValues: SqliteParser.Select_or_valuesContext) {
    if (selectOrValues.K_SELECT() != null) {
      // : K_SELECT ( K_DISTINCT | K_ALL )? result_column ( ',' result_column )*
      //   ( K_FROM ( table_or_subquery ( ',' table_or_subquery )* | join_clause ) )?
      //   ( K_WHERE expr )?
      //   ( K_GROUP K_BY expr ( ',' expr )* having_stmt? )?
      var validatedExpression = 0
      if (selectOrValues.K_WHERE() != null) {
        // First expression is the where clause which has access to scoped variables.
        resolver.resolve(selectOrValues.expr(0))
        validatedExpression++
      }

      if (selectOrValues.K_GROUP() != null) {
        // Group by clause does not have access to scoped variables.
        selectOrValues.expr().drop(validatedExpression).forEach { resolver.resolve(it) }
      }

      if (selectOrValues.having_stmt() != null) {
        resolver.resolve(selectOrValues.having_stmt().expr())
      }
    } else if (selectOrValues.K_VALUES() != null) {
      // | K_VALUES '(' expr ( ',' expr )* ')' ( ',' '(' expr ( ',' expr )* ')' )*
      validate(selectOrValues.values())
    }
  }

  fun validate(valuesContext: SqliteParser.ValuesContext) {
    valuesContext.expr().forEach { resolver.resolve(it) }

    if (valuesContext.values() != null) {
      validate(valuesContext.values())
    }
  }
}
