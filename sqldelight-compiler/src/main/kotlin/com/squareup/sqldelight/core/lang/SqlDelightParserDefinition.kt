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

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.SqldelightParserUtil

class SqlDelightParserDefinition: SqlParserDefinition() {
  private var dialect: DialectPreset? = null

  override fun createFile(viewProvider: FileViewProvider) = SqlDelightFile(viewProvider)
  override fun getFileNodeType() = FILE
  override fun getLanguage() = SqlDelightLanguage

  override fun createParser(project: Project): SqlParser {
    val newDialect = SqlDelightProjectService.getInstance(project).dialectPreset
    if (newDialect != dialect) {
      newDialect.setup()
      SqldelightParserUtil.overrideSqlParser()
      dialect = newDialect
    }
    return super.createParser(project)
  }

  companion object {
    private val FILE = IFileElementType(SqlDelightLanguage)
  }
}