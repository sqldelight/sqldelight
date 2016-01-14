package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SqliteCompiler
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingRegistry

class SqliteFileType private constructor() : LanguageFileType(SqliteLanguage.INSTANCE) {
  override fun getName() = "Sqlite"
  override fun getDescription() = "Sqlite"
  override fun getDefaultExtension() = SqliteCompiler.FILE_EXTENSION
  override fun getIcon() = ICON
  override fun getCharset(file: VirtualFile, content: ByteArray) =
      (EncodingRegistry.getInstance().getDefaultCharsetForPropertiesFiles(file) ?:
          CharsetToolkit.getDefaultSystemCharset()).name()

  companion object {
    private val ICON = IconLoader.getIcon("/icons/sqlite.png")

    val INSTANCE: LanguageFileType = SqliteFileType()
  }
}
