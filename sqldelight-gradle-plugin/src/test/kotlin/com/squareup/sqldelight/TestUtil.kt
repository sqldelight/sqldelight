package com.squareup.sqldelight

import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.gradle.SqlDelightCompilationUnitImpl
import com.squareup.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import com.squareup.sqldelight.gradle.SqlDelightPropertiesFileImpl
import com.squareup.sqldelight.gradle.SqlDelightSourceFolderImpl
import java.io.File

internal fun String.withInvariantPathSeparators() = replace("\\", "/")

internal fun SqlDelightPropertiesFileImpl.withInvariantPathSeparators(): SqlDelightPropertiesFile {
  return SqlDelightPropertiesFileImpl(
    databases = databases.map { it.withInvariantPathSeparators() }
  )
}

internal fun SqlDelightDatabasePropertiesImpl.withInvariantPathSeparators(): SqlDelightDatabasePropertiesImpl {
  return copy(
    compilationUnits = compilationUnits.map { it.withInvariantPathSeparators() },
    outputDirectoryFile = File(outputDirectoryFile.path.withInvariantPathSeparators())
  )
}

internal fun SqlDelightDatabasePropertiesImpl.withSortedCompilationUnits(): SqlDelightDatabasePropertiesImpl {
  return copy(
    compilationUnits = compilationUnits.map { it.withSortedSourceFolders() }
  )
}

private fun SqlDelightCompilationUnitImpl.withInvariantPathSeparators(): SqlDelightCompilationUnitImpl {
  return copy(sourceFolders = sourceFolders.map { it.withInvariantPathSeparators() })
}

private fun SqlDelightCompilationUnitImpl.withSortedSourceFolders(): SqlDelightCompilationUnitImpl {
  return copy(sourceFolders = sourceFolders.sortedBy { it.folder.path })
}

private fun SqlDelightSourceFolderImpl.withInvariantPathSeparators() =
  copy(folder = File(folder.path.withInvariantPathSeparators()))
