package com.squareup.sqldelight.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

fun Project.linkSqlite() {
  val extension = project.extensions.findByType(KotlinMultiplatformExtension ::class.java) ?: return
  extension.targets
    .filterIsInstance<KotlinNativeTarget>()
    .flatMap { it.binaries }
    .forEach { it.linkerOpts("-lsqlite3") }
}
