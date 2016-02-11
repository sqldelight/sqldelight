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

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.UPPER_CAMEL
import com.squareup.sqldelight.SqliteCompiler.Companion.FILE_EXTENSION
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper.getVariantDataManager

class SqlDelightPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.all({
      when (it) {
        is AppPlugin -> configureAndroid(project, it)
        is LibraryPlugin -> configureAndroid(project, it)
      }
    })
  }

  private fun configureAndroid(project: Project, plugin: BasePlugin) {
    val generateSqlite = project.task("generateSqliteInterface")

    val compileDeps = project.configurations.getByName("compile").dependencies
    project.gradle.addListener(object : DependencyResolutionListener {
      override fun beforeResolve(dependencies: ResolvableDependencies?) {
        compileDeps.add(
            project.dependencies.create("com.android.support:support-annotations:23.1.1"))
        project.gradle.removeListener(this)
      }

      override fun afterResolve(dependencies: ResolvableDependencies?) { }
    })

    project.afterEvaluate {
      getVariantDataManager(plugin).variantDataList.filter({ it.sourceGenTask != null }).forEach {
        // Set up the generateSql task.
        val taskName = "generate${LOWER_CAMEL.to(UPPER_CAMEL,
            it.name)}SqliteInterface"
        val task = project.tasks.create<SqlDelightTask>(taskName, SqlDelightTask::class.java)
        task.group = "sqlite"
        task.buildDirectory = project.buildDir
        task.description = "Generate Android interfaces for working with ${it.name} sqlite tables"
        task.source("src")
        task.include("**/*.$FILE_EXTENSION")

        generateSqlite.dependsOn(task)

        // Update the variant to include the sqlite task.
        it.registerJavaGeneratingTask(task, task.outputDirectory)
        it.variantConfiguration.sortedSourceProviders
            .filterIsInstance<AndroidSourceSet>()
            .forEach { (it.java as DefaultAndroidSourceDirectorySet).srcDir(task.outputDirectory) }
      }
    }
  }
}
