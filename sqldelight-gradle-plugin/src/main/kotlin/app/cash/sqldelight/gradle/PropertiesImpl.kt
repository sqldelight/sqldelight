package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.core.SqlDelightSourceFolder
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import java.io.File

data class SqlDelightPropertiesFileImpl(
  override val databases: List<SqlDelightDatabasePropertiesImpl>,
  override val dialectJars: Collection<File>,
  override val minimumSupportedVersion: String,
  override val currentVersion: String,
  override val moduleJars: Collection<File> = emptySet(),
) : SqlDelightPropertiesFile

data class SqlDelightDatabasePropertiesImpl(
  @Input override val packageName: String,
  @Nested val compilationUnitsProvider: Provider<List<SqlDelightCompilationUnitImpl>>,
  @Input override val className: String,
  @Nested override val dependencies: List<SqlDelightDatabaseNameImpl>,
  @Input override val deriveSchemaFromMigrations: Boolean = false,
  @Input override val treatNullAsUnknownForEquality: Boolean = false,
  @Input override val generateAsync: Boolean = false,
  // Only used by intellij plugin to help with resolution.
  @Internal override val rootDirectory: File,
) : SqlDelightDatabaseProperties {
  override val compilationUnits: List<SqlDelightCompilationUnitImpl> 
    get() = compilationUnitsProvider.get()
}

data class SqlDelightDatabaseNameImpl(
  @Input override val packageName: String,
  @Input override val className: String,
) : SqlDelightDatabaseName

data class SqlDelightCompilationUnitImpl(
  @Input override val name: String,
  @Nested val sourceDirectories: Provider<List<SqlDelightSourceFolderImpl>>,
  // Output directory is already cached [SqlDelightTask.outputDirectory].
  @Internal val outputDirectory: Provider<Directory>,
) : SqlDelightCompilationUnit {
  override val sourceFolders: List<SqlDelightSourceFolderImpl> 
    get() = sourceDirectories.get()
  override val outputDirectoryFile: File
    get() = outputDirectory.get().asFile
}

data class SqlDelightSourceFolderImpl(
  // Sources are already cached [SqlDelightTask.getSources]
  @Internal val directory: Directory,
  @Input override val dependency: Boolean = false,
) : SqlDelightSourceFolder {
  override val folder: File
    get() = directory.asFile
}
