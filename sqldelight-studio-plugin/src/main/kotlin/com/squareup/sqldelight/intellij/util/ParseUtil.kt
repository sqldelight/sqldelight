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
package com.squareup.sqldelight.intellij.util

import com.squareup.sqldelight.SqliteParser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext

internal fun RuleContext.containingParse(): SqliteParser.ParseContext =
  when (this) {
    is SqliteParser.ParseContext -> this
    else -> this.parent.containingParse()
  }

/**
 * Returns the smallest rule at the given index.
 */
internal fun ParserRuleContext.leafAt(index: Int): ParserRuleContext {
  for (i in 0..childCount) {
    val child = getChild(i)
    if (child is ParserRuleContext && child.start.startIndex <= index && child.stop.stopIndex > index) {
      return child.leafAt(index)
    }
  }
  return this
}

/**
 * @return true if this element is a identifier definition (Example: the table_name rule
 * in a create table statement).
 */
internal fun ParserRuleContext.isDefinition(): Boolean {
  when (this) {
    is SqliteParser.Table_nameContext -> {
      return parent is SqliteParser.Create_table_stmtContext
          || parent is SqliteParser.Common_table_expressionContext
    }
    is SqliteParser.Column_nameContext -> {
      return parent is SqliteParser.Column_defContext
    }
    is SqliteParser.View_nameContext -> {
      return parent is SqliteParser.Create_view_stmtContext
    }
    is SqliteParser.Column_aliasContext -> {
      return parent is SqliteParser.Result_columnContext
    }
    is SqliteParser.Table_aliasContext -> {
      return parent is SqliteParser.Table_or_subqueryContext
    }
    else -> return false
  }
}