package com.squareup.sqldelight.core

import com.alecstrong.sql.psi.core.DialectPreset
import java.io.File

data class SqlDelightPropertiesFileImpl(
  override val databases: List<SqlDelightDatabasePropertiesImpl>
) : SqlDelightPropertiesFile

data class SqlDelightDatabasePropertiesImpl(
  override val packageName: String,
  override val compilationUnits: List<SqlDelightCompilationUnitImpl>,
  override val className: String,
  override val dependencies: List<SqlDelightDatabaseNameImpl>,
  override val dialectPresetName: String = DialectPreset.SQLITE_3_18.name,
  override val deriveSchemaFromMigrations: Boolean = false,
  override val outputDirectoryFile: List<SqlDelightSourceDirectory>,
  override val rootDirectory: File
) : SqlDelightDatabaseProperties

data class SqlDelightDatabaseNameImpl(
  override val packageName: String,
  override val className: String
) : SqlDelightDatabaseName

data class SqlDelightCompilationUnitImpl(
  override val name: String,
  override val sourceFolders: List<SqlDelightSourceFolderImpl>
) : SqlDelightCompilationUnit

data class SqlDelightSourceFolderImpl(
  override val folder: File,
  override val dependency: Boolean = false
) : SqlDelightSourceFolder
