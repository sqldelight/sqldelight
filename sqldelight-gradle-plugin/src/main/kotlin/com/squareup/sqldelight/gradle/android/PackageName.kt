package com.squareup.sqldelight.gradle.android

import com.android.build.gradle.BaseExtension
import com.android.ide.common.symbols.getPackageNameFromManifest
import org.gradle.api.Project

internal fun Project.packageName(): String {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  androidExtension.sourceSets
    .map { it.manifest.srcFile }
    .filter { it.exists() }
    .forEach {
      return getPackageNameFromManifest(it)
    }
  throw IllegalStateException("No source sets available")
}

internal fun Project.sqliteVersion(): String? {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  val minSdk = androidExtension.defaultConfig.minSdk ?: return null
  if (minSdk >= 30) return "sqlite:3.25"
  return "sqlite:3.18"
}
