package app.cash.sqldelight.gradle.android

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.decapitalize
import app.cash.sqldelight.gradle.SqlDelightExtension
import app.cash.sqldelight.gradle.SqlDelightTask
import app.cash.sqldelight.gradle.kotlin.Source
import app.cash.sqldelight.gradle.setupDependencies
import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal fun Project.configureAndroid(extension: SqlDelightExtension) {
  AndroidPlugin.entries.forEach {
    plugins.withId(it.id) {
      val androidExtension = checkNotNull(extensions.findByType(AndroidComponentsExtension::class.java)) {
        "Could not find the Android Gradle Plugin extension for $name."
      }
      setupDependencies("api", extension)
      configureDatabases(extension)
      when {
        useModernApi(androidExtension.pluginVersion) -> {
          androidExtension.onVariants { variant ->
            configureAndroidVariant(variant, extension)
          }
        }
        else -> configureLegacyAndroidVariants(extension)
      }
    }
  }
}

private fun Project.configureAndroidVariant(
  variant: Component,
  extension: SqlDelightExtension,
) {
  val sources = checkNotNull(variant.sources.java) {
    "Could not find Kotlin/Java source folders for $name"
  }
  extension.databases.configureEach { database ->
    database.registerTasksForSource(
      source = Source(
        type = KotlinPlatformType.androidJvm,
        name = variant.name,
        sourceSets = variant.sourceSetNames(),
      ),
      registerGeneratedDirectory = { task ->
        sources.addGeneratedSourceDirectory(task, SqlDelightTask::outputDirectory)
      },
    )
  }
}

// AndroidComponentsExtension doesn't expose sourceSets in the same manner as BaseVariant.
// We can't use `Component.sources.kotlin.all` as this includes generated directories, which
// would create a circular dependency (SqlDelightTask would read its output as an input).
// Using `Component.sources.kotlin.static` would prevent that issue, but would only include
// source sets with actual Kotlin sources configured.
//
// This follows the legacy AGP VariantSources.getSortedSourceProviders() convention.
private fun Component.sourceSetNames(): Set<String> = buildSet {
  add("main")
  // Individual flavors (ex. minApi21, demo)
  productFlavors.forEach { (_, flavor) -> add(flavor) }
  // Multi-flavor combination with different dimensions (ex. minApi21Demo)
  productFlavors
    .joinToString("") { (_, flavor) -> flavor.capitalize() }
    .decapitalize()
    .takeUnless { it.isBlank() }
    ?.let { add(it) }

  // Individual build types (ex. debug, release)
  buildType?.let { add(it) }
  // Variant name combining flavors and build type (ex. minApi21DemoDebug)
  add(name)
}

private fun Project.configureDatabases(
  extension: SqlDelightExtension,
) = with(extension) {
  afterEvaluate {
    if (databases.isEmpty()) {
      // Default to a database for android named "Database" to keep things simple.
      databases.create("Database")
    }
  }

  databases.configureEach { database ->
    database.packageName.convention(project.packageNameProvider())
    database.dialectProperty.convention(project.sqliteVersionProvider())
  }
}

private enum class AndroidPlugin(val id: String) {
  Application("com.android.application"),
  Library("com.android.library"),
}

// The `addGeneratedSourceDirectory` method was broken until 8.12.0-alpha06.
// See https://issuetracker.google.com/issues/327399383
private fun useModernApi(
  currentVersion: AndroidPluginVersion,
): Boolean = currentVersion >= AndroidPluginVersion(8, 12, 0).alpha(6)
