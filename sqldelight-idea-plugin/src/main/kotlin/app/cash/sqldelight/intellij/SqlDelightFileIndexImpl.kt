package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.lang.SqlDelightFile
import com.intellij.openapi.vfs.VirtualFile

internal class SqlDelightFileIndexImpl : SqlDelightFileIndex {
  override val isConfigured
    get() = false
  override val packageName = ""
  override val className
    get() = throw UnsupportedOperationException()
  override val contentRoot
    get() = throw UnsupportedOperationException()
  override val dependencies: List<SqlDelightDatabaseName>
    get() = throw UnsupportedOperationException()
  override val deriveSchemaFromMigrations = false

  override fun outputDirectory(file: SqlDelightFile) = throw UnsupportedOperationException()
  override fun outputDirectories() = throw UnsupportedOperationException()

  override fun packageName(file: SqlDelightFile) = ""
  override fun sourceFolders(
    file: VirtualFile,
    includeDependencies: Boolean
  ) = listOf(file.parent)
  override fun sourceFolders(
    file: SqlDelightFile,
    includeDependencies: Boolean
  ) = listOfNotNull(file.parent)
}
