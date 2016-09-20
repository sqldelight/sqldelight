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
import com.squareup.sqldelight.types.ArgumentType

internal class CreateIndexValidator(val resolver: Resolver) {
  fun validate(index: SqliteParser.Create_index_stmtContext) {
    val resolution = listOf(resolver.resolve(index.table_name())).filterNotNull()
    index.indexed_column().forEach { resolver.resolve(resolution, it.column_name()) }

    if (index.expr() != null) {
      resolver.withScopedValues(resolution).resolve(index.expr(), expectedType = ArgumentType.boolean(index.expr()))
    }
  }
}
