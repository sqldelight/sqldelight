package app.cash.sqldelight.gradle.kotlin

import app.cash.sqldelight.gradle.SqlDelightDatabase
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File

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
internal fun SqlDelightDatabase.sources(): List<Source> {
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
    return (it as BaseExtension).sources()
  }

  // Kotlin project.
  val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
  return listOf(
    Source(
      type = KotlinPlatformType.jvm,
      name = "main",
      sourceSets = listOf("main"),
      sourceDirectorySet = sourceSets.getByName("main").kotlin!!,
    ),
  )
}

private fun KotlinJsProjectExtension.sources(): List<Source> {
  return listOf(
    Source(
      type = KotlinPlatformType.js,
      name = "main",
      sourceDirectorySet = sourceSets.getByName("main").kotlin,
      sourceSets = listOf("main"),
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
      nativePresetName = null,
      name = "commonMain",
      variantName = "commonMain",
      sourceDirectorySet = sourceSets.getByName("commonMain").kotlin,
      sourceSets = listOf("commonMain"),
    ),
  )
}

private fun BaseExtension.sources(): List<Source> {
  val variants: DomainObjectSet<out BaseVariant> = when (this) {
    is AppExtension -> applicationVariants
    is LibraryExtension -> libraryVariants
    else -> throw IllegalStateException("Unknown Android plugin $this")
  }
  val sourceSets = sourceSets
    .associate { sourceSet ->
      sourceSet.name to sourceSet.kotlinSourceDirectorySet
    }

  return variants.map { variant ->
    Source(
      type = KotlinPlatformType.androidJvm,
      name = variant.name,
      variantName = variant.name,
      sourceDirectorySet = sourceSets[variant.name]
        ?: throw IllegalStateException("Couldn't find ${variant.name} in $sourceSets"),
      sourceSets = variant.sourceSets.map { it.name },
      registerGeneratedDirectory = { outputDirectoryProvider ->
        variant.addJavaSourceFoldersToModel(outputDirectoryProvider.get())
      },
    )
  }
}

private fun TaskContainer.namedOrNull(
  taskName: String,
): TaskProvider<Task>? {
  return try {
    named(taskName)
  } catch (_: Exception) {
    null
  }
}

internal data class Source(
  val type: KotlinPlatformType,
  val nativePresetName: String? = null,
  val sourceDirectorySet: SourceDirectorySet,
  val name: String,
  val variantName: String? = null,
  val sourceSets: List<String>,
  val registerGeneratedDirectory: ((Provider<File>) -> Unit)? = null,
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
