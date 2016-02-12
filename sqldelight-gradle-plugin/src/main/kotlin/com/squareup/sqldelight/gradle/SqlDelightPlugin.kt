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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.squareup.sqldelight.SqliteCompiler.Companion.FILE_EXTENSION
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies

class SqlDelightPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.all {
      when (it) {
        is AppPlugin -> configureAndroid(project,
            project.extensions.getByType(AppExtension::class.java).applicationVariants)
        is LibraryPlugin -> configureAndroid(project,
            project.extensions.getByType(LibraryExtension::class.java).libraryVariants)
      }
    }
  }

  private fun <T : BaseVariant> configureAndroid(project: Project, variants: DomainObjectSet<T>) {
    val generateSqlDelight = project.task("generateSqlDelightInterface")

    val compileDeps = project.configurations.getByName("compile").dependencies
    project.gradle.addListener(object : DependencyResolutionListener {
      override fun beforeResolve(dependencies: ResolvableDependencies?) {
        if (System.getProperty("sqldelight.skip.runtime") != "true") {
          compileDeps.add(project.dependencies.create("com.squareup.sqldelight:runtime:0.1"))
        }
        compileDeps.add(
            project.dependencies.create("com.android.support:support-annotations:23.1.1"))

        project.gradle.removeListener(this)
      }

      override fun afterResolve(dependencies: ResolvableDependencies?) { }
    })

    variants.all {
      val taskName = "generate${it.name.capitalize()}SqlDelightInterface"
      val task = project.tasks.create<SqlDelightTask>(taskName, SqlDelightTask::class.java)
      task.group = "sqldelight"
      task.buildDirectory = project.buildDir
      task.description = "Generate Android interfaces for working with ${it.name} database tables"
      task.source("src")
      task.include("**/*.$FILE_EXTENSION")

      generateSqlDelight.dependsOn(task)

      it.registerJavaGeneratingTask(task, task.outputDirectory)
    }
  }
}
