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
package app.cash.sqldelight.gradle

import app.cash.sqldelight.VERSION
import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightEnvironment.CompilationStatus.Failure
import app.cash.sqldelight.core.SqlDelightException
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel.ERROR
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.util.ServiceLoader

@Suppress("UnstableApiUsage") // Worker API
@CacheableTask
abstract class SqlDelightTask : SqlDelightWorkerTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:OutputDirectory
  var outputDirectory: File? = null

  @Input val projectName: Property<String> = project.objects.property(String::class.java)

  @Nested lateinit var properties: SqlDelightDatabasePropertiesImpl
  @Nested lateinit var compilationUnit: SqlDelightCompilationUnitImpl

  @Input var verifyMigrations: Boolean = false

  @TaskAction
  fun generateSqlDelightFiles() {
    workQueue().submit(GenerateInterfaces::class.java) {
      it.outputDirectory.set(outputDirectory)
      it.projectName.set(projectName)
      it.properties.set(properties)
      it.verifyMigrations.set(verifyMigrations)
      it.compilationUnit.set(compilationUnit)
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface GenerateInterfacesWorkParameters : WorkParameters {
    val outputDirectory: DirectoryProperty
    val projectName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
    val compilationUnit: Property<SqlDelightCompilationUnit>
    val verifyMigrations: Property<Boolean>
  }

  abstract class GenerateInterfaces : WorkAction<GenerateInterfacesWorkParameters> {
    private val logger = Logging.getLogger(SqlDelightTask::class.java)

    override fun execute() {
      parameters.outputDirectory.get().asFile.deleteRecursively()
      val environment = SqlDelightEnvironment(
        compilationUnit = parameters.compilationUnit.get(),
        properties = parameters.properties.get(),
        moduleName = parameters.projectName.get(),
        verifyMigrations = parameters.verifyMigrations.get(),
        dialect = ServiceLoader.load(SqlDelightDialect::class.java).findFirst().get(),
      )

      val generationStatus = environment.generateSqlDelightFiles { info ->
        logger.log(INFO, info)
      }

      when (generationStatus) {
        is Failure -> {
          logger.log(ERROR, "")
          generationStatus.errors.forEach { logger.log(ERROR, it) }
          throw SqlDelightException(
            "Generation failed; see the generator error output for details."
          )
        }
      }
    }
  }
}
