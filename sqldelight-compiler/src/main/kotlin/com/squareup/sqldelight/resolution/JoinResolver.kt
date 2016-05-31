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
import com.squareup.sqldelight.validation.JoinValidator


/**
 * Take a join rule and return a list of the available columns.
 * Join rules look like
 *   FROM table_a JOIN table_b ON table_a.column_a = table_b.column_a
 */
internal fun Resolver.resolve(joinClause: SqliteParser.Join_clauseContext): Resolution {
  // Joins are complex because they are in a partial resolution state: They know about
  // values up to the point of this join but not afterward. Because of this, a validation step
  // for joins must happen as part of the resolution step.

  // Grab the values from the initial table or subquery (table_a in javadoc)
  var response = resolve(joinClause.table_or_subquery(0))

  joinClause.table_or_subquery().drop(1).zip(joinClause.join_constraint()) { table, constraint ->
    var localResponse = resolve(table)
    localResponse += Resolution(
        errors = JoinValidator(this, localResponse.values, response.values + scopedValues)
            .validate(constraint)
    )
    response += localResponse
  }
  return response
}