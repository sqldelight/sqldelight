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
package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.SqliteParser.Column_constraintContext
import com.squareup.sqldelight.SqliteParser.Column_defContext
import com.squareup.sqldelight.SqliteParser.Create_table_stmtContext
import com.squareup.sqldelight.SqliteParser.ParseContext
import com.squareup.sqldelight.SqliteParser.Sql_stmtContext
import com.squareup.sqldelight.TableGenerator
import com.squareup.sqldelight.model.Column.Type
import com.squareup.sqldelight.model.ColumnConstraint
import com.squareup.sqldelight.model.ColumnConstraint.NotNullConstraint
import com.squareup.sqldelight.model.SqlStmt.Replacement
import org.antlr.v4.runtime.ParserRuleContext

class TableGenerator
internal constructor(relativePath: String, parseContext: ParseContext, projectPath: String)
: TableGenerator<ParserRuleContext, Sql_stmtContext, Create_table_stmtContext, Column_defContext, Column_constraintContext>
(parseContext, relativePath, projectPath) {
  override fun sqlStatementElements(originatingElement: ParserRuleContext) =
      when (originatingElement) {
        is ParseContext -> originatingElement.sql_stmt_list().sql_stmt();
        else -> emptyList<Sql_stmtContext>()
      }

  override fun tableElement(sqlStatementElement: ParserRuleContext) =
      (sqlStatementElement as? ParseContext)?.sql_stmt_list()?.create_table_stmt()

  override fun identifier(sqlStatementElement: Sql_stmtContext) =
      sqlStatementElement.sql_stmt_name().text

  override fun columnElements(tableElement: Create_table_stmtContext) = tableElement.column_def()
  override fun tableName(tableElement: Create_table_stmtContext) = tableElement.table_name().text
  override fun columnName(columnElement: Column_defContext) = columnElement.column_name().text
  override fun classLiteral(columnElement: Column_defContext) =
      columnElement.type_name().sqlite_class_name()?.STRING_LITERAL()?.text

  override fun typeName(columnElement: Column_defContext) =
      when {
        columnElement.type_name().sqlite_class_name() != null ->
          columnElement.type_name().sqlite_class_name().getChild(0).text
        else -> columnElement.type_name().sqlite_type_name().text
      }

  override fun replacementFor(columnElement: Column_defContext, type: Type) =
      Replacement(columnElement.type_name().start.startIndex,
          columnElement.type_name().stop.stopIndex + 1, type.replacement)

  override fun constraintElements(columnElement: Column_defContext) =
      columnElement.column_constraint()

  override fun constraintFor(constraintElement: Column_constraintContext,
      replacements: List<Replacement>): ColumnConstraint<ParserRuleContext>? =
      when {
        constraintElement.K_NOT() != null -> NotNullConstraint(constraintElement);
        else -> null
      }

  override fun startOffset(sqliteStatementElement: ParserRuleContext) =
      when (sqliteStatementElement) {
        is Create_table_stmtContext -> sqliteStatementElement.start.startIndex
        else -> (sqliteStatementElement.getChild(
            sqliteStatementElement.childCount - 1) as ParserRuleContext).start.startIndex
      }

  override fun text(
      sqliteStatementElement: ParserRuleContext) = sqliteStatementElement.textWithWhitespace()
}
