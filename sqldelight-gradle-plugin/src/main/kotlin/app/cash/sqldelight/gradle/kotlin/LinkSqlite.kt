package app.cash.sqldelight.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun Project.linkSqlite(linkSqlite: Provider<Boolean>) {
  // https://youtrack.jetbrains.com/issue/KT-30081/Gradle-MPP-NativeBinary.linkerOpts-improvements
  afterEvaluate {
    if (linkSqlite.getOrElse(true)) {
      val extension = project.extensions.findByType(KotlinMultiplatformExtension::class.java) 
        ?: return@afterEvaluate
      extension.targets
        .filterIsInstance<KotlinNativeTarget>()
        .flatMap { it.binaries }
        .forEach { compilationUnit ->
          compilationUnit.linkerOpts("-lsqlite3")
        }
    }
  }
}
