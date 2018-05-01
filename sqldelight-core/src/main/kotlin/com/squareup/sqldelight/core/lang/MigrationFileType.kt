package com.squareup.sqldelight.core.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader

object MigrationFileType : LanguageFileType(MigrationLanguage) {
  private val ICON = try {
    IconLoader.getIcon("/icons/sqldelight.png")
  } catch (e : Throwable) {
    null
  }

  const val EXTENSION = "sqm"

  override fun getName() = "SqlDelight Migration"
  override fun getDescription() = "SqlDelight Migration"
  override fun getDefaultExtension() = EXTENSION
  override fun getIcon() = ICON
}