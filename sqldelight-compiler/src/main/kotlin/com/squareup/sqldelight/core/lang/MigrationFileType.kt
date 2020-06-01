package com.squareup.sqldelight.core.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType

object MigrationFileType : LanguageFileType(MigrationLanguage) {
  private val ICON = AllIcons.Providers.Sqlite

  const val EXTENSION = "sqm"

  override fun getName() = "SqlDelight Migration"
  override fun getDescription() = "SqlDelight Migration"
  override fun getDefaultExtension() = EXTENSION
  override fun getIcon() = ICON
}
