package com.squareup.tools.sqldelight.cli

import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightEnvironment
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Logger
import java.util.stream.Collectors.toList

class CompilerWrapper(
  packageName: String,
  private val outputDirectory: File,
  private val moduleName: String
) {
  private val logger = Logger.getLogger(CompilerWrapper::class.java.name)
  private val defaultProperties = SqlDelightDatabaseProperties(
    packageName = packageName,
    compilationUnits = emptyList(),
    outputDirectory = outputDirectory.toString(),
    className = "Database",
    dependencies = emptyList()
  )

  fun generate(srcFolders: List<File>): List<Path> {
    val environment = SqlDelightEnvironment(
      sourceFolders = srcFolders,
      dependencyFolders = emptyList(),
      properties = defaultProperties,
      outputDirectory = outputDirectory,
      moduleName = moduleName
    )

    when (val generationStatus = environment.generateSqlDelightFiles(logger::info)) {
      is SqlDelightEnvironment.CompilationStatus.Failure -> {
        generationStatus.errors.forEach { logger.severe(it) }
        throw SqlDelightException("Generation failed; see the generator error output for details.")
      }
    }
    val outPath = outputDirectory.toPath()
    return Files
      .walk(outPath)
      .filter { Files.isRegularFile(it) }
      .collect(toList())
  }
}
