package com.squareup.sqldelight.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation

fun Project.linkSqlite() {
  val extension = project.extensions.findByType(KotlinMultiplatformExtension ::class.java) ?: return
  extension.targets
      .flatMap { it.compilations }
      .filterIsInstance<KotlinNativeCompilation>()
      .forEach { compilationUnit ->
        compilationUnit.kotlinOptions.freeCompilerArgs += arrayOf("-linker-options", "-lsqlite3")
      }
}