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
package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.CustomSqliteParser
import com.alecstrong.sqlite.psi.core.SqliteParserDefinition
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase.Parser
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.squareup.sqldelight.core.SqlDelightParser
import com.squareup.sqldelight.core.SqlDelightTypes

class SqlDelightParserDefinition: SqliteParserDefinition() {
  init {
    setParserOverride(object : CustomSqliteParser() {
      override fun columnDef(builder: PsiBuilder, level: Int, column_def: Parser): Boolean {
        return SqlDelightParser.column_def(builder, level)
      }

      override fun sqlStmtList(builder: PsiBuilder, level: Int, sql_stmt_list: Parser): Boolean {
        return SqlDelightParser.sql_stmt_list(builder, level)
      }

      override fun typeName(builder: PsiBuilder, level: Int, type_name: Parser): Boolean {
        return SqlDelightParser.type_name(builder, level)
      }

      override fun insertStmt(builder: PsiBuilder, level: Int, insert_stmt: Parser): Boolean {
        return SqlDelightParser.insert_stmt(builder, level)
      }

      override fun createElement(node: ASTNode): PsiElement {
        return try {
          SqlDelightTypes.Factory.createElement(node)
        } catch (e: AssertionError) {
          super.createElement(node)
        }
      }
    })
  }

  override fun createFile(viewProvider: FileViewProvider) = SqlDelightFile(viewProvider)
  override fun getFileNodeType() = FILE
  override fun getLanguage() = SqlDelightLanguage

  companion object {
    private val FILE = IFileElementType(SqlDelightLanguage)
  }
}