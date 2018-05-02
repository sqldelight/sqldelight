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
import com.android.builder.core.DefaultManifestParser
import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import java.io.File

class SqlDelightAndroidPlugin : SqlDelightPlugin() {
  override fun apply(project: Project) {
    project.plugins.all {
      when (it) {
        is AppPlugin -> {
          val extension = project.extensions.getByType(AppExtension::class.java)
          configureAndroid(project, extension.applicationVariants)
        }
        is LibraryPlugin -> {
          val extension = project.extensions.getByType(LibraryExtension::class.java)
          configureAndroid(project, extension.libraryVariants)
        }
      }
    }
  }

  private fun <T : BaseVariant> configureAndroid(project: Project, variants: DomainObjectSet<T>) {
    val compileDeps = project.configurations.getByName("api").dependencies
    if (System.getProperty("sqldelight.skip.runtime") != "true") {
      compileDeps.add(project.dependencies.create("com.squareup.sqldelight:sqldelight-runtime:$VERSION"))
    }
    compileDeps.add(
        project.dependencies.create("com.android.support:support-annotations:23.1.1"))

    var packageName: String? = null
    val sourceSets = mutableListOf<List<String>>()
    val buildDirectory = listOf("generated", "source", "sqldelight").fold(project.buildDir, ::File)

    variants.all {
      val taskName = "generate${it.name.capitalize()}SqlDelightInterface"
      val task = project.tasks.create(taskName, SqlDelightTask::class.java)
      task.group = "sqldelight"
      task.outputDirectory = buildDirectory
      task.description = "Generate Android interfaces for working with ${it.name} database tables"
      task.source(it.sourceSets.map { "src/${it.name}/${SqlDelightFileType.FOLDER_NAME}" })
      task.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
      task.packageName = it.packageName()
      task.sourceFolders = it.sourceSets.map { File("${project.projectDir}/src/${it.name}/${SqlDelightFileType.FOLDER_NAME}") }

      sourceSets.add(task.sourceFolders.map { it.toRelativeString(project.projectDir) })
      packageName = task.packageName

      it.registerJavaGeneratingTask(task, task.outputDirectory)
    }

    project.afterEvaluate {
      val ideaDir = File(project.rootDir, ".idea")
      if (ideaDir.exists()) {
        val propsDir =
          File(ideaDir, "sqldelight/${project.projectDir.toRelativeString(project.rootDir)}")
        propsDir.mkdirs()

        val properties = SqlDelightPropertiesFile(
            packageName = packageName!!,
            sourceSets = sourceSets,
            outputDirectory = buildDirectory.toRelativeString(project.projectDir)
        )
        properties.toFile(File(propsDir, SqlDelightPropertiesFile.NAME))
      }

      addMigrationTasks(
          project = project,
          sourceSet = sourceSets.flatten().distinct().map { File(project.projectDir, it) },
          schemaOutputDirectory = File(project.projectDir, "src/main/sqldelight")
      )
    }
  }

  /**
   * Theres no external api to get the package name. There is to get the application id, but thats
   * the post build package for the play store, and not the package name that should be used during
   * compilation. Think R.java, we want to be using the same namespace as it.
   *
   * There IS an internal api for doing this.
   * [BaseVariantImpl.getVariantData().getVariantConfiguration().getPackageFromManifest()],
   * and so this code just emulates that behavior.
   *
   * Package name is enforced identical by agp across multiple source sets, so taking the first
   * package name we find is fine.
   */
  private fun BaseVariant.packageName(): String {
    return sourceSets.map { it.manifestFile }
        .filter { it.exists() }
        .mapNotNull { DefaultManifestParser(it, { true }).`package` }
        .first()
  }
}