package com.squareup.sqldelight

import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.SqlDelightSourceFolder

internal fun String.withInvariantPathSeparators() = replace("\\", "/")

internal fun SqlDelightPropertiesFile.withInvariantPathSeparators(): SqlDelightPropertiesFile {
  return SqlDelightPropertiesFile(
      databases = databases.map { it.withInvariantPathSeparators() }
  )
}

internal fun SqlDelightDatabaseProperties.withInvariantPathSeparators(): SqlDelightDatabaseProperties {
  return copy(
      compilationUnits = compilationUnits.map { it.withInvariantPathSeparators() },
      outputDirectory = outputDirectory.withInvariantPathSeparators()
  )
}

internal fun SqlDelightDatabaseProperties.withSortedCompilationUnits(): SqlDelightDatabaseProperties {
  return copy(
      compilationUnits = compilationUnits.map { it.withSortedSourceFolders() }
  )
}

private fun SqlDelightCompilationUnit.withInvariantPathSeparators(): SqlDelightCompilationUnit {
  return copy(sourceFolders = sourceFolders.map { it.withInvariantPathSeparators() })
}

private fun SqlDelightCompilationUnit.withSortedSourceFolders(): SqlDelightCompilationUnit {
  return copy(sourceFolders = sourceFolders.sortedBy { it.path })
}

private fun SqlDelightSourceFolder.withInvariantPathSeparators() = copy(path = path.withInvariantPathSeparators())