package app.cash.sqldelight.gradle.kotlin

import app.cash.sqldelight.gradle.SqlDelightDatabase
import app.cash.sqldelight.gradle.SqlDelightTask
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
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
    (it as AndroidComponentsExtension<*, *, *>).onSources(project, action)
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
      sourceDirectories = project.provider { listOf(project.sqldelightDirectory("commonMain")) },
      registerSourceGeneration = { task ->
        sourceSets.getByName("commonMain").kotlin.srcDir(task)
      },
    ),
  )
}

private fun AndroidComponentsExtension<*, *, *>.onSources(project: Project, action: (Source) -> Unit) {
  registerSourceType("sqldelight")

  onVariants { variant ->
    val source = Source(
      type = KotlinPlatformType.androidJvm,
      name = variant.name,
      buildType = variant.buildType,
      flavours = variant.productFlavors,
      sourceDirectories = variant.sources.getByName("sqldelight").all,
      registerSourceGeneration = { task ->
        when {
          pluginVersion < AndroidPluginVersion(8, 12, 0) -> {
            // AGP 8.9-8.11 addGeneratedSourceDirectory overwrites the task's
            // @OutputDirectory (DirectoryProperty.set vs .convention), so AGP registers
            // an empty path as the variant source. Fixed in AGP 8.12.0. Bypass AGP and
            // wire directly into KGP's variant source set.
            val kotlinSourceSets =
              project.extensions.getByType(KotlinProjectExtension::class.java).sourceSets
            kotlinSourceSets.named(variant.name) { it.kotlin.srcDir(task) }
          }
          pluginVersion < AndroidPluginVersion(9, 0) -> {
            // kotlin-android on AGP 8.x only wires AndroidSourceSet.java into Kotlin
            // compilation; variant.sources.kotlin registrations are ignored. AGP 9.0's
            // built-in Kotlin makes variant.sources.kotlin the canonical path.
            // https://issuetracker.google.com/issues/446220448
            variant.sources.java?.addGeneratedSourceDirectory(task, SqlDelightTask::outputDirectory)
          }
          else -> {
            variant.sources.kotlin?.addGeneratedSourceDirectory(task, SqlDelightTask::outputDirectory)
          }
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
  val buildType: String? = null,
  val flavours: List<Pair<String, String>>? = null,
  val sourceDirectories: Provider<out Collection<Directory>>,
  val registerSourceGeneration: (TaskProvider<SqlDelightTask>) -> Unit,
) {
  internal data class Key(
    val type: KotlinPlatformType,
    val name: String,
    val buildType: String?,
    val flavours: List<Pair<String, String>>?,
    val nativePresetName: String?,
  )

  val key get() = Key(type, name, buildType, flavours, nativePresetName)
}
