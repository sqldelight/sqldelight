package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.lang.MigrationFileType
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.gradle.kotlin.Source
import com.squareup.sqldelight.gradle.kotlin.sources
import groovy.lang.GroovyObject
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import java.io.File

class SqlDelightDatabase(
  val project: Project,
  var name: String,
  var packageName: String? = null,
  var schemaOutputDirectory: File? = null,
  var sourceFolders: Collection<String>? = null
) {
  private val outputDirectory
    get() = File(project.buildDir, "sqldelight/$name")

  private val sources by lazy { sources() }

  fun methodMissing(name: String, args: Any): Any {
    return (project as GroovyObject).invokeMethod(name, args)
  }

  internal fun getProperties(): SqlDelightDatabaseProperties {
    val packageName = requireNotNull(packageName) { "property packageName must be provided" }

    return SqlDelightDatabaseProperties(
        packageName = packageName,
        compilationUnits = sources.map { source ->
          return@map SqlDelightCompilationUnit(
              name = source.name,
              sourceFolders = relativeSourceFolders(source)
          )
        },
        outputDirectory = outputDirectory.toRelativeString(project.projectDir),
        className = name
    )
  }

  internal fun registerTasks() {
    val packageName = requireNotNull(packageName) { "property packageName must be provided" }
    val isMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")

    // Add the runtime dependency.
    if (isMultiplatform) {
      val sourceSets =
          project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
      val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
      project.configurations.getByName(sourceSet.apiConfigurationName).dependencies.add(
          project.dependencies.create("com.squareup.sqldelight:runtime:$VERSION")
      )
    } else {
      project.configurations.getByName("api").dependencies.add(
          project.dependencies.create("com.squareup.sqldelight:runtime-jvm:$VERSION")
      )
    }

    sources.forEach { source ->
      // Add the source dependency on the generated code.
      source.sourceDirectorySet.srcDir(outputDirectory.toRelativeString(project.projectDir))

      val sourceFiles = project.files(*relativeSourceFolders(source).map(::File).toTypedArray())

      // Register the sqldelight generating task.
      val task = project.tasks.register("generate${source.name.capitalize()}${name}Interface", SqlDelightTask::class.java) {
        it.packageName = packageName
        it.className = name
        it.sourceFolders = sourceFiles.files
        it.outputDirectory = outputDirectory
        it.source(sourceFiles)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = "sqldelight"
        it.description = "Generate ${source.name} Kotlin interface for $name"
      }

      // Register the task as a dependency of source compilation.
      source.registerTaskDependency(task)

      addMigrationTasks(sourceFiles.files, source)
    }
  }

  private fun relativeSourceFolders(source: Source): List<String> {
    val sourceFolders = sourceFolders ?: listOf("sqldelight")

    return sourceFolders.flatMap { folder ->
      source.sourceSets.map { "src/$it/$folder" }
    }
  }

  private fun addMigrationTasks(
    sourceSet: Collection<File>,
    source: Source
  ) {
    val verifyMigrationTask =
        project.tasks.register("verify${source.name.capitalize()}${name}Migration", VerifyMigrationTask::class.java) {
          it.sourceFolders = sourceSet
          it.source(sourceSet)
          it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
          it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
          it.group = "sqldelight"
          it.description = "Verify ${source.name} $name migrations and CREATE statements match."
        }

    if (schemaOutputDirectory != null) {
      project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
        it.sourceFolders = sourceSet
        it.outputDirectory = schemaOutputDirectory
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = "sqldelight"
        it.description = "Generate a .db file containing the current $name schema for ${source.name}."
      }
    }
    project.tasks.named("check").configure {
      it.dependsOn(verifyMigrationTask)
    }
  }
}