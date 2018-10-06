package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.sql.DriverManager

open class GenerateSchemaTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  @get:OutputDirectory var outputDirectory: File? = null

  lateinit var sourceFolders: Iterable<File>

  @TaskAction
  fun generateSchemaFile() {
    val environment = SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() }
    )

    var maxVersion = 1

    environment.forMigrationFiles { migrationFile ->
      maxVersion = maxOf(maxVersion, migrationFile.version + 1)
    }

    DriverManager.getConnection("jdbc:sqlite:$outputDirectory/$maxVersion.db").use { connection ->
      val sourceFiles = ArrayList<SqlDelightFile>()
      environment.forSourceFiles { file -> sourceFiles.add(file as SqlDelightFile) }
      sourceFiles.forInitializationStatements { sqlText ->
        connection.prepareStatement(sqlText).execute()
      }
    }
  }
}