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
package com.squareup.sqldelight.model

import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.util.javadocText
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.TerminalNode

fun ParserRuleContext.textWithWhitespace(): String {
  return if (start == null || stop == null || start.startIndex < 0 || stop.stopIndex < 0) text
  else start.inputStream.getText(Interval(start.startIndex, stop.stopIndex))
}

internal val SqliteParser.Sql_stmtContext.identifier: String
  get() = SqliteCompiler.constantName(sql_stmt_name().text)

internal fun SqliteParser.Sql_stmtContext.body() = getChild(childCount - 1) as ParserRuleContext

fun ParserRuleContext.sqliteText(): String {
  val text = textWithWhitespace()
  var nextOffset = 0
  return replacements()
      .fold(StringBuilder(), { builder, replacement ->
        builder.append(text.subSequence(nextOffset, replacement.startOffset - start.startIndex))
            .append(replacement.replacementText)
        nextOffset = replacement.endOffset - start.startIndex
        builder
      })
      .append(text.substring(nextOffset, text.length))
      .toString()
}

private fun ParserRuleContext.replacements(): Collection<Replacement> {

  if (this is SqliteParser.Type_nameContext && K_AS() != null) {
    return listOf(Replacement(K_AS().symbol.startIndex - 1, java_type_name().stop.stopIndex + 1, ""))
  }
  if (this is SqliteParser.Sql_stmtContext) {
    return listOf(Replacement(
            sql_stmt_name().start.startIndex,
            (getChild(2) as? ParserRuleContext)?.start?.startIndex ?: sql_stmt_name().stop.stopIndex,
            ""
    ))
  }
  var replacements:List<Replacement> = emptyList();
  if (this is SqliteParser.Create_table_stmtContext && JAVADOC_COMMENT() != null) {
    replacements += Replacement(JAVADOC_COMMENT().symbol.startIndex, K_CREATE().symbol.startIndex, "");
  }
  if (this is SqliteParser.Column_defContext && JAVADOC_COMMENT() != null) {
    replacements += Replacement(JAVADOC_COMMENT().symbol.startIndex, column_name().start.startIndex, "");
  }
  if (children != null) replacements += children.filterIsInstance<ParserRuleContext>().flatMap { it.replacements() }.toList()
  return replacements
}

private class Replacement(val startOffset: Int, val endOffset: Int, val replacementText: String)

internal fun SqliteParser.Sql_stmtContext.javadocText(): String? {
  return javadocText(JAVADOC_COMMENT());
}
