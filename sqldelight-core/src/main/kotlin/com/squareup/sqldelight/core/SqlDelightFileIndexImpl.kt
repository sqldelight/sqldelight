package com.squareup.sqldelight.core

import com.intellij.openapi.vfs.VirtualFile
import com.squareup.sqldelight.core.lang.SqlDelightFile

class SqlDelightFileIndexImpl : SqlDelightFileIndex {
  override val isConfigured
    get() = false
  override val outputDirectory
    get() = throw UnsupportedOperationException()
  override val packageName
    get() = throw UnsupportedOperationException()
  override val contentRoot
    get() = throw UnsupportedOperationException()
  override fun packageName(file: SqlDelightFile) = throw UnsupportedOperationException()
  override fun sourceFolders(file: VirtualFile) = listOf(file.parent)
  override fun sourceFolders(file: SqlDelightFile) = listOfNotNull(file.parent)
}