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
package app.cash.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.ILightStubFileElementType

class SqlDelightParserDefinition : SqlParserDefinition() {
  private val parserUtil = ParserUtil()

  override fun createFile(viewProvider: FileViewProvider) = SqlDelightQueriesFile(viewProvider)
  override fun getFileNodeType() = FILE
  override fun getLanguage() = SqlDelightLanguage

  override fun createElement(node: ASTNode): PsiElement {
    try {
      return super.createElement(node)
    } catch (e: Throwable) {
      throw IllegalStateException(
        """
        Failed to create element for node ${node.text}
          Element type was ${node.elementType}
          Dialect is currently ${parserUtil.dialect}
        """.trimIndent(),
        e,
      )
    }
  }

  override fun createParser(project: Project): SqlParser {
    parserUtil.initializeDialect(project)
    return super.createParser(project)
  }

  companion object {
    private val FILE = object : ILightStubFileElementType<PsiFileStub<SqlFileBase>>(SqlDelightLanguage) {
      override fun getExternalId(): String = "SqlDelight"
    }
  }
}
