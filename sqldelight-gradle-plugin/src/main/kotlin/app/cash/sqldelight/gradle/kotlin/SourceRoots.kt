package app.cash.sqldelight.gradle.kotlin

import app.cash.sqldelight.gradle.SqlDelightExtension
import app.cash.sqldelight.gradle.setupDependencies
import org.gradle.api.Project
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
internal fun Project.configureKotlin(extension: SqlDelightExtension) {
  warnOnEmptyDatabases(extension)
  // Multiplatform project.
  configureKotlinMultiplatform(extension)
  // kotlin.js only projects
  configureKotlinJs(extension)
  // Kotlin project.
  configureKotlinJvm(extension)
}

private fun Project.configureKotlinMultiplatform(
  extension: SqlDelightExtension,
) = plugins.withId(KotlinPlugin.Multiplatform.id) {
  val multiplatformExtension = checkNotNull(extensions.findByType(KotlinMultiplatformExtension::class.java)) {
    "Could not find the Kotlin Multiplatform extension for $name."
  }

  multiplatformExtension.linkSqliteIfEnabled(extension.linkSqlite)
  setupDependencies("commonMainApi", extension)
  val commonMain = multiplatformExtension.sourceSets.getByName("commonMain")
  extension.databases.configureEach { database ->
    database.isMultiplatform.set(true)
    // For multiplatform we only support SQLDelight in commonMain - to support other source sets
    // we would need to generate expect/actual SQLDelight code which at least right now doesn't
    // seem like there is a use case for. However this code is capable of running on any Target type.
    database.registerTasksForSource(
      source = Source(
        type = KotlinPlatformType.common,
        name = "commonMain",
        sourceSets = setOf("commonMain"),
      ),
      registerGeneratedDirectory = { task -> commonMain.kotlin.srcDir(task) },
    )
  }
}

private fun Project.configureKotlinJs(
  extension: SqlDelightExtension,
) = plugins.withId(KotlinPlugin.Js.id) {
  val jsExtension = checkNotNull(extensions.findByType(KotlinJsProjectExtension::class.java)) {
    "Could not find the Kotlin Js extension for $name."
  }
  setupDependencies("api", extension)
  val main = jsExtension.sourceSets.getByName("main")
  extension.databases.configureEach { database ->
    database.registerTasksForSource(
      source = Source(
        type = KotlinPlatformType.js,
        name = "main",
        sourceSets = setOf("main"),
      ),
      registerGeneratedDirectory = { task -> main.kotlin.srcDir(task) },
    )
  }
}

private fun Project.configureKotlinJvm(
  extension: SqlDelightExtension,
) = plugins.withId(KotlinPlugin.Jvm.id) {
  val sourceSets = (extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets
  setupDependencies("api", extension)
  val main = sourceSets.getByName("main")
  extension.databases.configureEach { database ->
    database.registerTasksForSource(
      source = Source(
        type = KotlinPlatformType.jvm,
        name = "main",
        sourceSets = setOf("main"),
      ),
      registerGeneratedDirectory = { task -> main.kotlin.srcDir(task) },
    )
  }
}

private fun Project.warnOnEmptyDatabases(
  extension: SqlDelightExtension,
) = with(extension) {
  afterEvaluate {
    if (databases.isEmpty()) {
      logger.warn("SQLDelight Gradle plugin was applied but there are no databases set up.")
    }
  }
}

internal enum class KotlinPlugin(val id: String) {
  Multiplatform("org.jetbrains.kotlin.multiplatform"),
  Js("org.jetbrains.kotlin.js"),
  Jvm("org.jetbrains.kotlin.jvm"),
}

internal data class Source(
  // Changing this to a string or a custom enum could enable using compileOnly
  // instead of implementation for the Kotlin Gradle plugin.
  val type: KotlinPlatformType,
  val name: String,
  val sourceSets: Set<String>,
)

// We only register the KMP source for KMP projects with the com.android.library plugin.
internal fun Source.isEnabled(isMultiplatform: Boolean): Boolean = !(type == KotlinPlatformType.androidJvm && isMultiplatform)
