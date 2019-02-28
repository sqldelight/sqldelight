package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseName
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightSourceFolder
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
  private val dependencies = mutableListOf<SqlDelightDatabase>()

  private var recursionGuard = false

  fun methodMissing(name: String, args: Any): Any {
    return (project as GroovyObject).invokeMethod(name, args)
  }

  fun dependency(dependencyProject: Project) {
    dependencyProject.afterEvaluate {
      val dependency = dependencyProject.extensions.findByType(SqlDelightExtension::class.java)
          ?: throw IllegalStateException("Cannot depend on a module with no sqldelight plugin.")
      val database = dependency.databases.singleOrNull { it.name == name }
          ?: throw IllegalStateException("No database named $name in $dependencyProject")
      if (database.packageName == packageName) {
        throw IllegalStateException("Detected a schema that already has the package name $packageName in project $dependencyProject")
      }
      dependencies.add(database)
    }
  }

  internal fun getProperties(): SqlDelightDatabaseProperties {
    val packageName = requireNotNull(packageName) { "property packageName must be provided" }

    if (recursionGuard) {
      throw IllegalStateException("Found a circular dependency in $project with database $name")
    }
    recursionGuard = true

    try {
      return SqlDelightDatabaseProperties(
          packageName = packageName,
          compilationUnits = sources.map { source ->
            return@map SqlDelightCompilationUnit(
                name = source.name,
                sourceFolders = sourceFolders(source)
            )
          },
          outputDirectory = outputDirectory.toRelativeString(project.projectDir),
          className = name,
          dependencies = dependencies.map { SqlDelightDatabaseName(it.packageName!!, it.name) }
      )
    } finally {
      recursionGuard = false
    }
  }

  private fun sourceFolders(source: Source): List<SqlDelightSourceFolder> {
    val sourceFolders = sourceFolders ?: listOf("sqldelight")

    val relativeSourceFolders = sourceFolders.flatMap { folder ->
      source.sourceSets.map { SqlDelightSourceFolder("src/$it/$folder", false) }
    }

    return relativeSourceFolders + dependencies.flatMap { dependency ->
      val dependencySource = source.closestMatch(dependency.sources)
          ?: return@flatMap emptyList<SqlDelightSourceFolder>()
      val compilationUnit = dependency.getProperties().compilationUnits
          .single { it.name == dependencySource.name }

      return@flatMap compilationUnit.sourceFolders.map {
        val folder = File(dependency.project.projectDir, it.path)
        return@map SqlDelightSourceFolder(project.relativePath(folder), true)
      }
    }
  }

  internal fun registerTasks() {
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

      val allFiles = sourceFolders(source)
      val sourceFiles = project.files(*allFiles.filter { !it.dependency }.map { File(it.path) }.toTypedArray())
      val dependencyFiles = project.files(*allFiles.filter { it.dependency }.map { File(it.path) }.toTypedArray())

      // Register the sqldelight generating task.
      val task = project.tasks.register("generate${source.name.capitalize()}${name}Interface", SqlDelightTask::class.java) {
        it.properties = getProperties()
        it.sourceFolders = sourceFiles.files
        it.dependencySourceFolders = dependencyFiles.files
        it.outputDirectory = outputDirectory
        it.source(sourceFiles + dependencyFiles)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = "sqldelight"
        it.description = "Generate ${source.name} Kotlin interface for $name"
      }

      // Register the task as a dependency of source compilation.
      source.registerTaskDependency(task)

      addMigrationTasks(sourceFiles.files + dependencyFiles.files, source)
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