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
package com.squareup.sqldelight

import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.Column.Type
import com.squareup.sqldelight.model.ColumnConstraint
import com.squareup.sqldelight.model.SqlElement
import com.squareup.sqldelight.model.SqlStmt
import com.squareup.sqldelight.model.SqlStmt.Replacement
import com.squareup.sqldelight.model.Table
import org.antlr.v4.runtime.ParserRuleContext
import java.io.File
import java.io.File.separatorChar
import java.util.ArrayList

class TableGenerator
constructor(
    rootElement: SqliteParser.ParseContext,
    relativeFile: String,
    private val projectPath: String
) : SqlElement(rootElement) {
  private val fileName = relativeFile.split(separatorChar).last()
  private val originalFileName =
      if (fileName.endsWith(SqliteCompiler.FILE_EXTENSION)) //
        fileName.substring(0, fileName.length - (SqliteCompiler.FILE_EXTENSION.length + 1))
      else fileName

  internal val table: Table?
  internal val sqliteStatements = ArrayList<SqlStmt>()
  internal val packageName = relativeFile.split(separatorChar).dropLast(1).joinToString(".")

  val generatedFileName = originalFileName + "Model"
  val outputDirectory = File(File(projectPath, "build"), SqliteCompiler.OUTPUT_DIRECTORY)
  val packageDirectory = packageName.replace('.', separatorChar)
  val fileDirectory = File(outputDirectory, packageDirectory)

  init {
    if (packageName.isEmpty()) {
      throw SqlitePluginException(rootElement, ".sq cannot be children of the sqldelight " +
          "container. Place them in their own package under sqldelight.")
    }
    var table: Table? = null
    try {
      val tableElement = rootElement.sql_stmt_list().create_table_stmt()
      if (tableElement != null) {
        val replacements = ArrayList<Replacement>()
        table = tableFor(tableElement, packageName, originalFileName, replacements)
        sqliteStatements.add(SqlStmt("create_table", text(tableElement),
            startOffset(tableElement), replacements, tableElement))
      }

      for (sqlStatementElement in rootElement.sql_stmt_list().sql_stmt()) {
        sqliteStatements.add(sqliteStatementFor(sqlStatementElement, arrayListOf()))
      }
    } catch (e: ArrayIndexOutOfBoundsException) {
      // Do nothing, just an easy way to catch a lot of situations where sql is incomplete.
      table = null
    }

    this.table = table
  }

  fun startOffset(sqliteStatementElement: ParserRuleContext) =
      when (sqliteStatementElement) {
        is SqliteParser.Create_table_stmtContext -> sqliteStatementElement.start.startIndex
        else -> (sqliteStatementElement.getChild(
            sqliteStatementElement.childCount - 1) as ParserRuleContext).start.startIndex
      }

  fun text(sqliteStatementElement: ParserRuleContext) = when (sqliteStatementElement) {
    is SqliteParser.Sql_stmtContext -> sqliteStatementElement.statementTextWithWhitespace()
    else -> sqliteStatementElement.textWithWhitespace()
  }

  private fun tableFor(tableElement: SqliteParser.Create_table_stmtContext, packageName: String,
      fileName: String, replacements: MutableList<Replacement>): Table {
    val table = Table(packageName, fileName, tableElement.table_name().text, tableElement)
    tableElement.column_def().forEach { table.columns.add(columnFor(it, replacements)) }
    return table
  }

  private fun columnFor(columnElement: SqliteParser.Column_defContext,
      replacements: MutableList<Replacement>): Column {
    val columnName = columnElement.column_name().text
    val type = Type.valueOf(typeName(columnElement))
    val constraints = columnElement.column_constraint()
        .map({ constraintFor(it, replacements) })
        .filterNotNull()
    val classLiteral = columnElement.type_name().sqlite_class_name()?.STRING_LITERAL()?.text?.filter { it != '\'' }
    val result = when (classLiteral) {
      null -> Column(columnName, type, constraints, originatingElement = columnElement)
      else -> Column(columnName, type, constraints,  classLiteral, columnElement)
    }

    replacements.add(Replacement(columnElement.type_name().start.startIndex,
            columnElement.type_name().stop.stopIndex + 1, type.replacement))

    return result
  }

  private fun sqliteStatementFor(sqliteStatementElement: SqliteParser.Sql_stmtContext,
      replacements: List<Replacement>): SqlStmt =
      SqlStmt(sqliteStatementElement.sql_stmt_name().text, text(sqliteStatementElement),
          startOffset(sqliteStatementElement), replacements, sqliteStatementElement)

  private fun constraintFor(constraintElement: SqliteParser.Column_constraintContext,
      replacements: List<Replacement>): ColumnConstraint? =
      when {
        constraintElement.K_NOT() != null -> ColumnConstraint.NotNullConstraint(constraintElement);
        else -> null
      }

  private fun typeName(columnElement: SqliteParser.Column_defContext) =
      when {
        columnElement.type_name().sqlite_class_name() != null ->
          columnElement.type_name().sqlite_class_name().getChild(0).text
        else -> columnElement.type_name().sqlite_type_name().text
      }
}

fun String.relativePath(originatingElement: ParserRuleContext): String {
  val parts = split(separatorChar)
  for (i in 2..parts.size) {
    if (parts[i - 2] == "src" && parts[i] == "sqldelight") {
      return parts.subList(i + 1, parts.size).joinToString(separatorChar.toString())
    }
  }
  throw SqlitePluginException(originatingElement,
      "Files must be organized like src/main/sqldelight/...")
}
