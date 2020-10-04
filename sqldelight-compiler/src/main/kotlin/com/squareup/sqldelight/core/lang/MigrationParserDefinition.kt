package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.ILightStubFileElementType

class MigrationParserDefinition : SqlParserDefinition() {
  private val parserUtil = ParserUtil()

  override fun createFile(viewProvider: FileViewProvider) = MigrationFile(viewProvider)
  override fun getFileNodeType() = FILE
  override fun getLanguage() = SqlDelightLanguage

  override fun createParser(project: Project): SqlParser {
    parserUtil.initializeDialect(project)
    return super.createParser(project)
  }

  companion object {
    private val FILE = ILightStubFileElementType<PsiFileStub<SqlFileBase>>(MigrationLanguage)
  }
}
