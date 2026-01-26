package app.cash.sqldelight.gradle.kotlin

import app.cash.sqldelight.gradle.SqlDelightExtension
import app.cash.sqldelight.gradle.setupDependencies
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal fun Project.configureKotlin(extension: SqlDelightExtension) {
  configureKotlinMultiplatform(extension)
  configureKotlinJs(extension)
  configureKotlinJvm(extension)
  warnOnEmptyDatabases(extension)
}

private fun Project.configureKotlinMultiplatform(
  extension: SqlDelightExtension,
) = pluginManager.withPlugin(KotlinPlugin.Multiplatform.id) {
  val multiplatformExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)
  multiplatformExtension.linkSqliteIfEnabled(extension.linkSqlite)
  setupDependencies("commonMainApi", extension)
  // For multiplatform we only support SQLDelight in commonMain - to support other source sets
  // we would need to generate expect/actual SQLDelight code which at least right now doesn't
  // seem like there is a use case for. However this code is capable of running on any Target type.
  val commonMain = multiplatformExtension.sourceSets.getByName("commonMain")
  extension.databases.configureEach { database ->
    database.isMultiplatform.set(true)
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
) = pluginManager.withPlugin(KotlinPlugin.Js.id) {
  val jsExtension = extensions.getByType(KotlinJsProjectExtension::class.java)
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
) = pluginManager.withPlugin(KotlinPlugin.Jvm.id) {
  val jvmExtension = extensions.getByType(KotlinJvmExtension::class.java)
  setupDependencies("api", extension)
  val main = jvmExtension.sourceSets.getByName("main")
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
) = afterEvaluate {
  if (extension.databases.isEmpty()) {
    logger.warn("SQLDelight Gradle plugin was applied but there are no databases set up.")
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

// We only register the KMP source for KMP/Android projects.
internal fun Source.isEnabled(isMultiplatform: Boolean): Boolean = !(type == KotlinPlatformType.androidJvm && isMultiplatform)
