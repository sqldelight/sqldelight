package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.SqliteParserDefinition
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType

class MigrationParserDefinition : SqliteParserDefinition() {
  override fun createFile(viewProvider: FileViewProvider) = MigrationFile(viewProvider)
  override fun getFileNodeType() = FILE
  override fun getLanguage() = SqlDelightLanguage

  companion object {
    private val FILE = IFileElementType(MigrationLanguage)
  }
}