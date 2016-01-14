package com.squareup.sqlite.android.gradle

import com.squareup.sqlite.android.SqliteCompiler
import com.squareup.sqlite.android.SqliteCompiler.Companion
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.google.common.base.CaseFormat
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.UPPER_CAMEL
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper.getVariantDataManager

class SqliteAndroidPlugin
@Inject
constructor(private val fileResolver: FileResolver) : Plugin<Project> {
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
        val sqliteSources = DefaultSourceDirectorySet(it.name, fileResolver)
        sqliteSources.filter.include("**/*." + SqliteCompiler.FILE_EXTENSION)
        sqliteSources.srcDirs("src")

        // Set up the generateSql task.
        val taskName = "generate${LOWER_CAMEL.to(UPPER_CAMEL,
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
        it.variantConfiguration.sortedSourceProviders
            .filterIsInstance<AndroidSourceSet>()
            .forEach { (it.java as DefaultAndroidSourceDirectorySet).srcDir(task.outputDirectory) }
      }
    }
  }
}
