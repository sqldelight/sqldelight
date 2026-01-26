@file:Suppress("Deprecation")

package app.cash.sqldelight.gradle.android

import app.cash.sqldelight.gradle.SqlDelightExtension
import app.cash.sqldelight.gradle.kotlin.Source
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

// This file can be removed when the min-supported agp >= 8.12.
internal fun Project.configureLegacyAndroidVariants(extension: SqlDelightExtension) {
  val androidExtension = checkNotNull(extensions.findByType(CommonExtension::class.java)) {
    "Could not find android extension for $name"
  }

  val variants: DomainObjectSet<out BaseVariant> = when (androidExtension) {
    is AppExtension -> androidExtension.applicationVariants
    is LibraryExtension -> androidExtension.libraryVariants
    else -> throw IllegalStateException("Unknown Android plugin $androidExtension")
  }

  variants.configureEach { variant ->
    val kotlinSourceSets = (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets
    val variantSourceSet = kotlinSourceSets.getByName(variant.name).kotlin
    extension.databases.configureEach { database ->
      database.registerTasksForSource(
        source = Source(
          type = KotlinPlatformType.androidJvm,
          name = variant.name,
          sourceSets = variant.sourceSets.map { it.name }.toSet(),
        ),
        registerGeneratedDirectory = { task ->
          variantSourceSet.srcDir(task)
          val outputDir = task.flatMap { it.outputDirectory.asFile }
          // The legacy path will have to wire this up eagerly.
          variant.addJavaSourceFoldersToModel(outputDir.get())
        },
      )
    }
  }
}
