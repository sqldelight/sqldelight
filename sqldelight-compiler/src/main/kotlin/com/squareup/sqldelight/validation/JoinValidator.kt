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

internal class JoinValidator(
    private val resolver: Resolver,
    private val values: List<Value>,
    private val scopedValues: List<Value>
) {
  fun validate(join: SqliteParser.Join_clauseContext) {
    // : table_or_subquery ( join_operator table_or_subquery join_constraint )*
    TableOrSubqueryValidator(resolver, values, scopedValues).validate(join.table_or_subquery(0))

    join.table_or_subquery().drop(1).zip(join.join_constraint(), { tableOrSubquery, constraint ->
      val tableOrSubqueryValues = resolver.resolve(tableOrSubquery)
      TableOrSubqueryValidator(resolver, tableOrSubqueryValues, values + scopedValues)
          .validate(tableOrSubquery)

      JoinValidator(resolver, tableOrSubqueryValues, values + scopedValues).validate(constraint)
    })
  }

  fun validate(joinConstraint: SqliteParser.Join_constraintContext) {
    if (joinConstraint.K_ON() != null) {
      // : ( K_ON expr
      ExpressionValidator(resolver, values + scopedValues).validate(joinConstraint.expr())
    }
    if (joinConstraint.K_USING() != null) {
      // | K_USING '(' column_name ( ',' column_name )* ')' )?
      joinConstraint.column_name().forEach { column_name ->
        // This column name must be in the scoped values (outside this join) and values (inside join)
        if (!values.any { it.columnName == column_name.text }) {
          throw SqlitePluginException(column_name, "Joined table or subquery does not contain " +
              "a column with the name ${column_name.text}")
        }
        if (!scopedValues.any { it.columnName == column_name.text }) {
          throw SqlitePluginException(column_name, "Table joined against does not contain " +
              "a column with the name ${column_name.text}")
        }
      }
    }
  }
}
