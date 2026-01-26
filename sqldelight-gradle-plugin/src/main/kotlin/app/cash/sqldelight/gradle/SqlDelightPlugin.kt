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
import app.cash.sqldelight.gradle.android.configureAndroid
import app.cash.sqldelight.gradle.kotlin.configureKotlin
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

internal const val MIN_GRADLE_VERSION = "8.0"

abstract class SqlDelightPlugin : Plugin<Project> {
  @get:Inject
  abstract val registry: ToolingModelBuilderRegistry

  override fun apply(project: Project) = with(project) {
    require(GradleVersion.current() >= GradleVersion.version(MIN_GRADLE_VERSION)) {
      "SQLDelight requires Gradle version $MIN_GRADLE_VERSION or greater."
    }

    dependencies.configureSqlDelightAttributesSchema()

    val extension = extensions.create("sqldelight", SqlDelightExtension::class.java).apply {
      linkSqlite.convention(true)
    }

    tasks.register("generateSqlDelightInterface") {
      it.group = GROUP
      it.description = "Aggregation task which runs every interface generation task for every given source"
    }

    tasks.register("verifySqlDelightMigration") {
      it.group = GROUP
      it.description = "Aggregation task which runs every migration task for every given source"
    }

    configureAndroid(extension)
    configureKotlin(extension)

    checkKotlinPluginApplied()
    registry.register(PropertiesModelBuilder(extension.databases))
  }

  private fun Project.checkKotlinPluginApplied() {
    val applied: Property<Boolean> = objects
      .property(Boolean::class.java)
      .convention(false)

    plugins.withType(KotlinBasePlugin::class.java).configureEach {
      applied.set(true)
    }

    afterEvaluate {
      check(applied.get()) {
        "SQL Delight Gradle plugin applied in project '$path' but no supported Kotlin plugin was found"
      }
    }
  }

  class PropertiesModelBuilder(
    private val databases: Iterable<SqlDelightDatabase>,
  ) : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean {
      return modelName == SqlDelightPropertiesFile::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
      return SqlDelightPropertiesFileImpl(
        databases = databases.map { it.resolveProperties() },
        currentVersion = VERSION,
        minimumSupportedVersion = MINIMUM_SUPPORTED_VERSION,
        dialectJars = databases.first().configuration.get().files,
      )
    }
  }

  internal companion object {
    const val GROUP = "sqldelight"
  }
}

internal fun Project.setupDependencies(
  apiConfigurationName: String,
  extension: SqlDelightExtension,
) {
  configurations.named(apiConfigurationName) { config ->
    config.dependencies.add(dependencies.create("app.cash.sqldelight:runtime:$VERSION"))
    config.dependencies.addLater(
      provider {
        if (extension.databases.any { it.generateAsync.get() }) {
          dependencies.create("app.cash.sqldelight:async-extensions:$VERSION")
        } else {
          null
        }
      },
    )
  }
}
