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
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.lang.MigrationFileType
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

open class SqlDelightPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    if (project.plugins.hasPlugin("android")) {
      throw IllegalStateException(
          """
      Android projects need to apply the sqldelight android plugin:

      buildscript {
        dependencies {
          classpath "com.squareup.sqldelight:android-gradle-plugin:$VERSION
        }
      }

      apply plugin: "com.squareup.sqldelight.android"
      """.trimIndent()
      )
    }

    val extension = project.extensions.create("sqldelight", SqlDelightExtension::class.java)
    val outputDirectory = File(project.buildDir, "sqldelight")

    val sourceSets = project.property("sourceSets") as SourceSetContainer
    sourceSets.getByName("main").kotlin!!.srcDirs(outputDirectory.toRelativeString(project.projectDir))

    project.afterEvaluate {
      val packageName = requireNotNull(extension.packageName) { "property packageName must be provided" }
      val sourceSet = extension.sourceSet ?: project.files("src/main/sqldelight")

      val ideaDir = File(project.rootDir, ".idea")
      if (ideaDir.exists()) {
        val propsDir =
          File(ideaDir, "sqldelight/${project.projectDir.toRelativeString(project.rootDir)}")
        propsDir.mkdirs()

        val properties = SqlDelightPropertiesFile(
            packageName = packageName,
            sourceSets = listOf(sourceSet.map { it.toRelativeString(project.projectDir) }),
            outputDirectory = outputDirectory.toRelativeString(project.projectDir)
        )
        properties.toFile(File(propsDir, SqlDelightPropertiesFile.NAME))
      }

      val task = project.tasks.create("generateSqlDelightInterface", SqlDelightTask::class.java) {
        it.packageName = packageName
        it.sourceFolders = sourceSet.files
        it.outputDirectory = outputDirectory
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
      }
      task.group = "sqldelight"
      task.description = "Generate Kotlin interfaces for .sq files"

      project.tasks.findByName("compileKotlin")?.dependsOn(task)
      project.tasks.findByName("compileKotlinCommon")?.dependsOn(task)

      addMigrationTasks(project, sourceSet.files, extension.schemaOutputDirectory)
    }
  }

  protected fun addMigrationTasks(
    project: Project,
    sourceSet: Collection<File>,
    schemaOutputDirectory: File?
  ) {
    val verifyMigrationTask = project.tasks.create("verifySqlDelightMigration", VerifyMigrationTask::class.java) {
      it.sourceFolders = sourceSet
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
      it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
    }

    verifyMigrationTask.group = "sqldelight"
    verifyMigrationTask.description = "Verify SQLDelight migrations and CREATE statements match."

    if (schemaOutputDirectory != null) {
      val generateSchemaTask =
        project.tasks.create("generateSqlDelightSchema", GenerateSchemaTask::class.java) {
          it.sourceFolders = sourceSet
          it.outputDirectory = schemaOutputDirectory
          it.source(sourceSet)
          it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
          it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        }
      generateSchemaTask.group = "sqldelight"
      generateSchemaTask.description = "Generate a .db file containing the current schema."
    }

    project.tasks.findByName("check")?.dependsOn(verifyMigrationTask)
  }

  // Copied from kotlin plugin
  private val SourceSet.kotlin: SourceDirectorySet?
    get() {
      val convention = (getConvention("kotlin") ?: getConvention("kotlin2js")) ?: return null
      val kotlinSourceSetIface = convention.javaClass.interfaces.find { it.name == KotlinSourceSet::class.qualifiedName }
      val getKotlin = kotlinSourceSetIface?.methods?.find { it.name == "getKotlin" } ?: return null
      return getKotlin(convention) as? SourceDirectorySet

    }

  private fun Any.getConvention(name: String): Any? =
    (this as HasConvention).convention.plugins[name]
}
