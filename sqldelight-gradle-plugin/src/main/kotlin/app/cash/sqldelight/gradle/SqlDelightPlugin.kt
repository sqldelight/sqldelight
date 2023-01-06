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
import app.cash.sqldelight.core.MINIMUM_SUPPORTED_VERSION
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.gradle.android.packageName
import app.cash.sqldelight.gradle.android.sqliteVersion
import app.cash.sqldelight.gradle.kotlin.linkSqlite
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import javax.inject.Inject

abstract class SqlDelightPlugin : Plugin<Project> {
  @get:Inject
  abstract val registry: ToolingModelBuilderRegistry

  override fun apply(project: Project) {
    require(GradleVersion.current() >= GradleVersion.version("7.0")) {
      "SQLDelight requires Gradle version 7.0 or greater"
    }

    val extension = project.extensions.create("sqldelight", SqlDelightExtension::class.java)
    registry.register(PropertiesModelBuilder(extension.databases))

    val androidPluginHandler = { _: Plugin<*> ->
      project.afterEvaluate {
        project.setupSqlDelightTasks(afterAndroid = true, extension)
      }
    }
    project.plugins.withId("com.android.application", androidPluginHandler)
    project.plugins.withId("com.android.library", androidPluginHandler)
    project.plugins.withId("com.android.instantapp", androidPluginHandler)
    project.plugins.withId("com.android.feature", androidPluginHandler)
    project.plugins.withId("com.android.dynamic-feature", androidPluginHandler)

    val kotlinPluginHandler = { _: Plugin<*> ->
      project.setupSqlDelightTasks(afterAndroid = false, extension)
    }
    project.plugins.withId("org.jetbrains.kotlin.multiplatform", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.android", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.jvm", kotlinPluginHandler)
    project.plugins.withId("org.jetbrains.kotlin.js", kotlinPluginHandler)
    project.plugins.withId("kotlin2js", kotlinPluginHandler)
  }

  private fun Project.setupSqlDelightTasks(afterAndroid: Boolean, extension: SqlDelightExtension) {
    val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    val isJsOnly = if (isMultiplatform) false else project.plugins.hasPlugin("org.jetbrains.kotlin.js")

    val needsAsyncRuntime = objects.setProperty(Boolean::class.java)
    extension.databases.configureEach {
      needsAsyncRuntime.add(it.generateAsync) 
    }
    val runtimeDependencies = needsAsyncRuntime.map {
      val runtime = project.dependencies.create("app.cash.sqldelight:runtime:$VERSION")
      if (it.contains(true)) {
        val asyncExtension = project.dependencies.create("app.cash.sqldelight:async-extensions:$VERSION")
        listOf(runtime, asyncExtension)
      } else listOf(runtime)
    }

    // Add the runtime dependency.
    when {
      isMultiplatform -> {
        val sourceSets =
          project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
        project.configurations.getByName(sourceSet.apiConfigurationName)
          .dependencies.addAllLater(runtimeDependencies)
      }
      isJsOnly -> {
        val sourceSets =
          project.extensions.getByType(KotlinJsProjectExtension::class.java).sourceSets
        val sourceSet = (sourceSets.getByName("main") as DefaultKotlinSourceSet)
        project.configurations.getByName(sourceSet.apiConfigurationName)
          .dependencies.addAllLater(runtimeDependencies)
      }
      else -> {
        project.configurations.getByName("api").dependencies.addAllLater(runtimeDependencies)
      }
    }
    
    project.linkSqlite(extension.linkSqlite)

    extension.run {
      if (afterAndroid && databases.isEmpty()) {
        // Default to a database for android named "Database" to keep things simple.
        databases.maybeRegister("Database") { db ->
          db.packageName.convention(project.packageName())
          project.sqliteVersion()?.let {
            db.dialect.convention(it)
          }
        }
      }

      project.tasks.maybeRegister("generateSqlDelightInterface") {
        it.group = GROUP
        it.description = "Aggregation task which runs every interface generation task for every given source"
      }

      project.tasks.maybeRegister("verifySqlDelightMigration") {
        it.group = GROUP
        it.description = "Aggregation task which runs every migration task for every given source"
      }

      databases.configureEach { database ->
        database.packageName.convention(project.packageName())
        if (afterAndroid) {
          project.sqliteVersion()?.let {
            database.dialect.convention(it)
          }
        } else {
          database.dialect.convention(project.dependencies.create("app.cash.sqldelight:sqlite-3-18-dialect:$VERSION"))
        }

        database.registerTasks()
      }
    }
  }

  class PropertiesModelBuilder(
    private val databases: NamedDomainObjectContainer<SqlDelightDatabase>,
  ) : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean {
      return modelName == SqlDelightPropertiesFile::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): SqlDelightPropertiesFile {
      return SqlDelightPropertiesFileImpl(
        databases = databases.map { it.properties().get() },
        currentVersion = VERSION,
        minimumSupportedVersion = MINIMUM_SUPPORTED_VERSION,
        dialectJars = databases.first().configuration.get().files,
        moduleJars = databases.first().moduleConfiguration.get().files,
      )
    }
  }

  internal companion object {
    const val GROUP = "sqldelight"
  }
}
