package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.core.SqlDelightSourceFolder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import java.io.File

data class SqlDelightPropertiesFileImpl(
  override val databases: List<SqlDelightDatabasePropertiesImpl>,
  override val dialectJar: File,
  override val minimumSupportedVersion: String,
  override val currentVersion: String,
) : SqlDelightPropertiesFile

data class SqlDelightDatabasePropertiesImpl(
  @Input override val packageName: String,
  @Nested override val compilationUnits: List<SqlDelightCompilationUnitImpl>,
  @Input override val className: String,
  @Nested override val dependencies: List<SqlDelightDatabaseNameImpl>,
  @Input override val deriveSchemaFromMigrations: Boolean = false,
  @Input override val treatNullAsUnknownForEquality: Boolean = false,
  // Only used by intellij plugin to help with resolution.
  @Internal override val rootDirectory: File
) : SqlDelightDatabaseProperties

data class SqlDelightDatabaseNameImpl(
  @Input override val packageName: String,
  @Input override val className: String
) : SqlDelightDatabaseName

data class SqlDelightCompilationUnitImpl(
  @Input override val name: String,
  @Nested override val sourceFolders: List<SqlDelightSourceFolderImpl>,
  // Output directory is already cached [SqlDelightTask.outputDirectory].
  @Internal override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit

data class SqlDelightSourceFolderImpl(
  // Sources are already cached [SqlDelightTask.getSources]
  @Internal override val folder: File,
  @Input override val dependency: Boolean = false
) : SqlDelightSourceFolder
