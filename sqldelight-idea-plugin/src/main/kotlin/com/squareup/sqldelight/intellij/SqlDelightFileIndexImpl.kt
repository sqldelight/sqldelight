package com.squareup.sqldelight.intellij

import com.intellij.openapi.vfs.VirtualFile
import com.squareup.sqldelight.core.SqlDelightDatabaseName
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile

internal class SqlDelightFileIndexImpl : SqlDelightFileIndex {
  override val isConfigured
    get() = false
  override val outputDirectory
    get() = throw UnsupportedOperationException()
  override val packageName
    get() = throw UnsupportedOperationException()
  override val className
    get() = throw UnsupportedOperationException()
  override val contentRoot
    get() = throw UnsupportedOperationException()
  override val dependencies: List<SqlDelightDatabaseName>
    get() = throw UnsupportedOperationException()
  override val deriveSchemaFromMigrations = false

  override fun packageName(file: SqlDelightFile) = throw UnsupportedOperationException()
  override fun sourceFolders(
    file: VirtualFile,
    includeDependencies: Boolean
  ) = listOf(file.parent)
  override fun sourceFolders(
    file: SqlDelightFile,
    includeDependencies: Boolean
  ) = listOfNotNull(file.parent)
}
