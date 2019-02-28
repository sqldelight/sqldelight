/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.SqlDelightEnvironment.CompilationStatus.Failure
import com.squareup.sqldelight.core.SqlDelightException
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel.ERROR
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class SqlDelightTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  @get:OutputDirectory var outputDirectory: File? = null

  @Internal lateinit var sourceFolders: Iterable<File>
  @Internal lateinit var dependencySourceFolders: Iterable<File>
  @Internal @Input lateinit var properties: SqlDelightDatabaseProperties

  @TaskAction
  fun generateSqlDelightFiles() {
    outputDirectory?.deleteRecursively()
    val environment = SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = dependencySourceFolders.filter { it.exists() },
        properties = properties,
        outputDirectory = outputDirectory
    )

    val generationStatus = environment.generateSqlDelightFiles { info ->
      logger.log(INFO, info)
    }

    when (generationStatus) {
      is Failure -> {
        logger.log(ERROR, "")
        generationStatus.errors.forEach { logger.log(ERROR, it) }
        throw SqlDelightException("Generation failed; see the generator error output for details.")
      }
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }
}
