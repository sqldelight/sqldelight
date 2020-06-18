package com.squareup.sqldelight.core.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile

object DatabaseFileType : FileType {
  const val EXTENSION = "db"

  override fun getName() = "SqlDelight Database"
  override fun getDescription() = "SqlDelight Database"
  override fun getDefaultExtension() = EXTENSION
  override fun getIcon() = AllIcons.Nodes.DataSchema
  override fun isBinary() = true
  override fun isReadOnly() = true
  override fun getCharset(vFile: VirtualFile, bytes: ByteArray) = null
}
