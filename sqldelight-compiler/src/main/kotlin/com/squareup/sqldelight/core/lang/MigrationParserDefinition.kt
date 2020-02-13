package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlParserDefinition
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType

class MigrationParserDefinition : SqlParserDefinition() {
  override fun createFile(viewProvider: FileViewProvider) = MigrationFile(viewProvider)
  override fun getFileNodeType() = FILE
  override fun getLanguage() = SqlDelightLanguage

  companion object {
    private val FILE = IFileElementType(MigrationLanguage)
  }
}