package app.cash.sqldelight

import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightPropertiesFileImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import java.io.File

internal fun String.withInvariantPathSeparators() = replace("\\", "/")

internal fun SqlDelightPropertiesFileImpl.withInvariantPathSeparators(): SqlDelightPropertiesFile {
  return copy(
    databases = databases.map { it.withInvariantPathSeparators() }
  )
}

internal fun SqlDelightDatabasePropertiesImpl.withInvariantPathSeparators(): SqlDelightDatabasePropertiesImpl {
  return copy(
    compilationUnits = compilationUnits.map { it.withInvariantPathSeparators() }
  )
}

internal fun SqlDelightDatabasePropertiesImpl.withSortedCompilationUnits(): SqlDelightDatabasePropertiesImpl {
  return copy(
    compilationUnits = compilationUnits.map { it.withSortedSourceFolders() }
  )
}

private fun SqlDelightCompilationUnitImpl.withInvariantPathSeparators(): SqlDelightCompilationUnitImpl {
  return copy(
    sourceFolders = sourceFolders.map { it.withInvariantPathSeparators() },
    outputDirectoryFile = File(outputDirectoryFile.path.withInvariantPathSeparators()),
  )
}

private fun SqlDelightCompilationUnitImpl.withSortedSourceFolders(): SqlDelightCompilationUnitImpl {
  return copy(sourceFolders = sourceFolders.sortedBy { it.folder.path })
}

private fun SqlDelightSourceFolderImpl.withInvariantPathSeparators() =
  copy(folder = File(folder.path.withInvariantPathSeparators()))
