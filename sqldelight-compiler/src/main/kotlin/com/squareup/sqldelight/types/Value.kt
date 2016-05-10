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
package com.squareup.sqldelight.types

import com.squareup.sqldelight.SqliteParser
import org.antlr.v4.runtime.ParserRuleContext

data class Value(
    val tableName: String?,
    val columnName: String?,
    internal val type: SqliteType,
    internal val element: ParserRuleContext,
    internal val tableNameElement: ParserRuleContext?
) {
  constructor(
      tableNameElement: ParserRuleContext?,
      column: SqliteParser.Column_defContext
  ) : this(
      tableNameElement?.text,
      column.column_name().text,
      SqliteType.valueOf(column.type_name().sqlite_type_name().text),
      column,
      tableNameElement
  )

  enum class SqliteType {
    INTEGER, REAL, TEXT, BLOB
  }
}

internal fun List<Value>.columns(columnName: String, tableName: String?) = filter {
  it.columnName != null && it.columnName == columnName && (tableName == null || it.tableName == tableName)
}
