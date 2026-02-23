package app.cash.sqldelight.gradle.kotlin

import app.cash.sqldelight.gradle.SqlDelightDatabase
import app.cash.sqldelight.gradle.SqlDelightTask
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * @return A list of source roots and their dependencies.
 *
 * Examples:
 *   Multiplatform Environment. Ios target labeled "ios".
 *     -> iosMain deps [commonMain]
 *
 *   Android environment. internal, production, release, debug variants.
 *     -> internalDebug deps [internal, debug, main]
 *     -> internalRelease deps [internal, release, main]
 *     -> productionDebug deps [production, debug, main]
 *     -> productionRelease deps [production, release, main]
 *
 *    Multiplatform environment with android target (oh boy)
 */
internal fun SqlDelightDatabase.sources(project: Project): List<Source> {
  // Multiplatform project.
  project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let {
    return it.sources()
  }

  // kotlin.js only projects
  project.extensions.findByType(KotlinJsProjectExtension::class.java)?.let {
    return it.sources()
  }

  // Android project.
  project.extensions.findByName("android")?.let {
    return (it as CommonExtension).sources(project)
  }

  // Kotlin project.
  val sourceSets = (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets
  return listOf(
    Source(
      type = KotlinPlatformType.jvm,
      name = "main",
      sourceDirectories = project.provider { listOf(project.sqldelightDirectory("main")) },
      registerSourceGeneration = { task ->
        sourceSets.getByName("main").kotlin.srcDir(task)
      },
    ),
  )
}

private fun KotlinJsProjectExtension.sources(): List<Source> {
  return listOf(
    Source(
      type = KotlinPlatformType.js,
      name = "main",
      sourceDirectories = project.provider { listOf(project.sqldelightDirectory("main")) },
      registerSourceGeneration = { task ->
        sourceSets.getByName("main").kotlin.srcDir(task)
      },
    ),
  )
}

private fun KotlinMultiplatformExtension.sources(): List<Source> {
  // For multiplatform we only support SQLDelight in commonMain - to support other source sets
  // we would need to generate expect/actual SQLDelight code which at least right now doesn't
  // seem like there is a use case for. However this code is capable of running on any Target type.
  return listOf(
    Source(
      type = KotlinPlatformType.common,
      nativePresetName = "common",
      name = "commonMain",
      variantName = null,
      sourceDirectories = project.provider { listOf(project.sqldelightDirectory("commonMain")) },
      registerSourceGeneration = { task ->
        sourceSets.getByName("commonMain").kotlin.srcDir(task)
      },
    ),
  )
}

private fun CommonExtension.sources(project: Project): List<Source> {
  val variants: DomainObjectSet<out BaseVariant> = when (this) {
    is AppExtension -> applicationVariants
    is LibraryExtension -> libraryVariants
    else -> throw IllegalStateException("Unknown Android plugin $this")
  }

  val kotlinSourceSets = (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets
  return variants.map { variant ->
    Source(
      type = KotlinPlatformType.androidJvm,
      name = variant.name,
      variantName = variant.name,
      sourceDirectories = project.provider { variant.sourceSets.map { project.sqldelightDirectory(it.name) } },
      registerSourceGeneration = { task ->
        kotlinSourceSets.getByName(variant.name).kotlin.srcDir(task)
        variant.addJavaSourceFoldersToModel(task.flatMap { it.outputDirectory.asFile }.get())
      },
    )
  }
}

private fun Project.sqldelightDirectory(sourceName: String): Directory {
  return project.layout.projectDirectory.dir("src/$sourceName/sqldelight")
}

internal data class Source(
  val type: KotlinPlatformType,
  val nativePresetName: String? = null,
  val name: String,
  val variantName: String? = null,
  val sourceDirectories: Provider<List<Directory>>,
  val registerSourceGeneration: (TaskProvider<SqlDelightTask>) -> Unit,
) {
  fun closestMatch(sources: Collection<Source>): Source? {
    var matches = sources.filter {
      type == it.type || (type == KotlinPlatformType.androidJvm && it.type == KotlinPlatformType.jvm) || it.type == KotlinPlatformType.common
    }
    if (matches.size <= 1) return matches.singleOrNull()

    // Multiplatform native matched or android variants matched.
    matches = matches.filter {
      nativePresetName == it.nativePresetName && variantName == it.variantName
    }
    return matches.singleOrNull()
  }
}
