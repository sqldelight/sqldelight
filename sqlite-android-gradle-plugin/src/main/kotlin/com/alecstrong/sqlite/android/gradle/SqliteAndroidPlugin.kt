package com.alecstrong.sqlite.android.gradle

import com.alecstrong.sqlite.android.SqliteCompiler
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.google.common.base.CaseFormat
import javax.inject.Inject
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.compile.JavaCompile

class SqliteAndroidPlugin
@Inject
constructor(private val fileResolver: FileResolver) : Plugin<Project> {
  override fun apply(project: Project) {
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

    project.afterEvaluate { afterEvaluateProject ->
      val variants = project.variants()
      if (variants.isEmpty()) throw IllegalStateException(
          "Apply the sqlite-android plugin after the android plugin")
      variants.all({
        val sqliteSources = DefaultSourceDirectorySet(it.name, fileResolver)
        sqliteSources.filter.include("**/*." + SqliteCompiler.getFileExtension())
        sqliteSources.srcDirs("src")

        // Set up the generateSql task.
        val taskName = "generate${CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
            it.name)}SqliteInterface"
        val task = project.tasks.create<SqliteAndroidTask>(taskName, SqliteAndroidTask::class.java)
        task.group = "sqlite"
        task.buildDirectory = project.buildDir
        task.description = "Generate Android interfaces for working with ${it.name} sqlite tables"
        task.setSource(sqliteSources)

        generateSqlite.dependsOn(task)

        // Update the variant to include the sqlite task.
        it.registerJavaGeneratingTask(task, task.outputDirectory)
        it.addJavaSourceFoldersToModel(task.outputDirectory)
        (it.javaCompiler as JavaCompile).options.compilerArgs.addAll(
            listOf("-sourcepath", task.outputDirectory.toString()))
      })
    }
  }

  private fun Project.variants(): DomainObjectSet<out BaseVariant> {
    if (!project.hasProperty("android")) {
      throw IllegalStateException("Must have applied the android plugin")
    }
    val android = project.property("android")
    return when (android) {
      is AppExtension -> android.applicationVariants;
      is LibraryExtension -> android.libraryVariants;
      else -> throw IllegalStateException("Android Property is neither an app nor a library")
    }
  }
}
