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
import java.io.File
import java.io.File.separatorChar
import java.util.ArrayList

abstract class TableGenerator<OriginatingType,
    SqliteStatementType : OriginatingType,
    TableType : OriginatingType,
    ColumnType : OriginatingType,
    ConstraintType : OriginatingType>
protected constructor(rootElement: OriginatingType, relativeFile: String,
    private val projectPath: String) : SqlElement<OriginatingType>(rootElement) {
  private val fileName = relativeFile.split(separatorChar).last()
  private val originalFileName =
      if (fileName.endsWith(SqliteCompiler.FILE_EXTENSION)) //
        fileName.substring(0, fileName.length - (SqliteCompiler.FILE_EXTENSION.length + 1))
      else fileName

  internal val table: Table<OriginatingType>?
  internal val sqliteStatements = ArrayList<SqlStmt<OriginatingType>>()
  internal val packageName = relativeFile.split(separatorChar).dropLast(1).joinToString(".")

  val generatedFileName = originalFileName + "Model"
  val outputDirectory = File(File(projectPath, "build"), SqliteCompiler.OUTPUT_DIRECTORY)
  val packageDirectory = packageName.replace('.', separatorChar)
  val fileDirectory = File(outputDirectory, packageDirectory)

  init {
    if (packageName.isEmpty()) {
      throw SqlitePluginException(rootElement as Any, ".sq cannot be children of the sqldelight " +
          "container. Place them in their own package under sqldelight.")
    }
    var table: Table<OriginatingType>? = null
    try {
      val tableElement = tableElement(rootElement)
      if (tableElement != null) {
        val replacements = ArrayList<Replacement>()
        table = tableFor(tableElement, packageName, originalFileName, replacements)
        sqliteStatements.add(SqlStmt<OriginatingType>("create_table", text(tableElement),
            startOffset(tableElement), replacements, tableElement))
      }

      for (sqlStatementElement in sqlStatementElements(rootElement)) {
        sqliteStatements.add(sqliteStatementFor(sqlStatementElement, arrayListOf()))
      }
    } catch (e: ArrayIndexOutOfBoundsException) {
      // Do nothing, just an easy way to catch a lot of situations where sql is incomplete.
      table = null
    }

    this.table = table
  }

  protected abstract fun sqlStatementElements(
      originatingElement: OriginatingType): Iterable<SqliteStatementType>

  protected abstract fun tableElement(sqlStatementElement: OriginatingType): TableType?
  protected abstract fun identifier(sqlStatementElement: SqliteStatementType): String
  protected abstract fun columnElements(tableElement: TableType): Iterable<ColumnType>
  protected abstract fun tableName(tableElement: TableType): String
  protected abstract fun columnName(columnElement: ColumnType): String
  protected abstract fun classLiteral(columnElement: ColumnType): String?
  protected abstract fun typeName(columnElement: ColumnType): String
  protected abstract fun replacementFor(columnElement: ColumnType, type: Type): Replacement
  protected abstract fun constraintElements(columnElement: ColumnType): Iterable<ConstraintType>
  protected abstract fun constraintFor(constraintElement: ConstraintType,
      replacements: List<Replacement>): ColumnConstraint<OriginatingType>?

  protected abstract fun text(sqliteStatementElement: OriginatingType): String
  protected abstract fun startOffset(sqliteStatementElement: OriginatingType): Int

  private fun tableFor(tableElement: TableType, packageName: String,
      fileName: String, replacements: MutableList<Replacement>): Table<OriginatingType> {
    val table = Table<OriginatingType>(packageName, fileName, tableName(tableElement), tableElement)
    columnElements(tableElement).forEach { table.columns.add(columnFor(it, replacements)) }
    return table
  }

  private fun columnFor(columnElement: ColumnType,
      replacements: MutableList<Replacement>): Column<OriginatingType> {
    val columnName = columnName(columnElement)
    val type = Type.valueOf(typeName(columnElement))
    val constraints = constraintElements(columnElement)
        .map({ constraintFor(it, replacements) })
        .filterNotNull()
    val result = when (classLiteral(columnElement)) {
      null -> Column(columnName, type, constraints, originatingElement = columnElement)
      else -> Column(columnName, type, constraints,  classLiteral(columnElement), columnElement)
    }

    replacements.add(replacementFor(columnElement, type))

    return result
  }

  private fun sqliteStatementFor(sqliteStatementElement: SqliteStatementType,
      replacements: List<Replacement>): SqlStmt<OriginatingType> =
      SqlStmt(identifier(sqliteStatementElement), text(sqliteStatementElement),
          startOffset(sqliteStatementElement), replacements, sqliteStatementElement)
}

fun String.relativePath(originatingElement: Any): String {
  val parts = split(separatorChar)
  for (i in 2..parts.size) {
    if (parts[i - 2] == "src" && parts[i] == "sqldelight") {
      return parts.subList(i + 1, parts.size).joinToString(separatorChar.toString())
    }
  }
  throw SqlitePluginException(originatingElement,
      "Files must be organized like src/main/sqldelight/...")
}
