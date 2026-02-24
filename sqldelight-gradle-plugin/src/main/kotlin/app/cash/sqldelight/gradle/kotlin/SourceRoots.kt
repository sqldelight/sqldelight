package app.cash.sqldelight.gradle.kotlin

import app.cash.sqldelight.gradle.SqlDelightDatabase
import app.cash.sqldelight.gradle.SqlDelightTask
import com.android.build.api.AndroidPluginVersion
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
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Invokes the [action] callback for every source root detected for the project.
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
internal fun SqlDelightDatabase.configureOnSources(action: (Source) -> Unit) {
  // Multiplatform project.
  project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { extension ->
    extension.sources().forEach { action(it) }
    return
  }

  // kotlin.js only projects
  project.extensions.findByType(KotlinJsProjectExtension::class.java)?.let { extension ->
    extension.sources().forEach { action(it) }
    return
  }

  project.extensions.findByName("androidComponents")?.let {
    (it as AndroidComponentsExtension<*, *, *>).onSources(action)
    return
  }

  // Kotlin project.
  project.extensions.findByType(KotlinProjectExtension::class.java)?.let { extension ->
    val sourceSets = extension.sourceSets
    val kotlinSource = Source(
      type = KotlinPlatformType.jvm,
      name = "main",
      sourceDirectories = project.provider { listOf(project.sqldelightDirectory("main")) },
      registerSourceGeneration = { task ->
        sourceSets.getByName("main").kotlin.srcDir(task)
      },
    )
    action(kotlinSource)
  }
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

private fun AndroidComponentsExtension<*, *, *>.onSources(action: (Source) -> Unit) {
  registerSourceType("sqldelight")

  onVariants { variant ->
    val source = Source(
      type = KotlinPlatformType.androidJvm,
      name = variant.name,
      variantName = variant.name,
      sourceDirectories = variant.sources.getByName("sqldelight").all,
      registerSourceGeneration = { task ->
        if (pluginVersion < AndroidPluginVersion(9, 0)) {
          // AGP versions before 9.0 wouldn't pick up the task dependency correctly when using the kotlin sources here,
          // so we use the java ones instead
          // https://issuetracker.google.com/issues/446220448
          variant.sources.java?.addGeneratedSourceDirectory(task, SqlDelightTask::outputDirectory)
        } else {
          variant.sources.kotlin?.addGeneratedSourceDirectory(task, SqlDelightTask::outputDirectory)
        }
      },
    )

    action(source)
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
  val sourceDirectories: Provider<out Collection<Directory>>,
  val registerSourceGeneration: (TaskProvider<SqlDelightTask>) -> Unit,
) {
  internal data class Key(
    val type: KotlinPlatformType,
    val name: String,
    val variantName: String?,
    val nativePresetName: String?,
  )

  val key get() = Key(type, name, variantName, nativePresetName)

  fun closestMatch(sources: Collection<Key>): Key? {
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
