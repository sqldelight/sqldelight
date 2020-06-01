package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.SqldelightParserUtil

class MigrationParserDefinition : SqlParserDefinition() {
  private var dialect: DialectPreset? = null

  override fun createFile(viewProvider: FileViewProvider) = MigrationFile(viewProvider)
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
    private val FILE = IFileElementType(MigrationLanguage)
  }
}
