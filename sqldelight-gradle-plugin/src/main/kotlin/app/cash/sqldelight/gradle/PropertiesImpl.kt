package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseOptions
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.core.SqlDelightSourceFolder
import java.io.File

data class SqlDelightPropertiesFileImpl(
  override val databases: List<SqlDelightDatabasePropertiesImpl>,
  override val dialectJars: Collection<File>,
  override val minimumSupportedVersion: String,
  override val currentVersion: String,
) : SqlDelightPropertiesFile

/**
 * The settings that decide what gets generated for a database and nothing else. Tasks declare
 * this whole value as an `@Input`, so it must not carry absolute paths or anything that varies
 * with how many variants a build happens to configure.
 */
data class SqlDelightDatabaseOptionsImpl(
  override val packageName: String,
  override val className: String,
  override val dependencies: List<SqlDelightDatabaseNameImpl>,
  override val deriveSchemaFromMigrations: Boolean = false,
  override val treatNullAsUnknownForEquality: Boolean = false,
  override val generateAsync: Boolean = false,
  override val expandSelectStar: Boolean = true,
  override val codegenExcludedColumns: Set<String> = emptySet(),
) : SqlDelightDatabaseOptions

/**
 * [options] plus the project layout needed to locate sources. The tooling model builds one with
 * every compilation unit for the IDE. Tasks build one with a single unit at execution time. This
 * should not be used as a task input since it carries absolute paths.
 */
data class SqlDelightDatabasePropertiesImpl(
  val options: SqlDelightDatabaseOptionsImpl,
  override val compilationUnits: List<SqlDelightCompilationUnitImpl>,
  // Only used by intellij plugin to help with resolution.
  override val rootDirectory: File,
) : SqlDelightDatabaseProperties,
  SqlDelightDatabaseOptions by options

data class SqlDelightDatabaseNameImpl(
  override val packageName: String,
  override val className: String,
) : SqlDelightDatabaseName

data class SqlDelightCompilationUnitImpl(
  override val name: String,
  override val sourceFolders: Set<SqlDelightSourceFolderImpl>,
  override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit

data class SqlDelightSourceFolderImpl(
  override val folder: File,
  override val dependency: Boolean = false,
) : SqlDelightSourceFolder
