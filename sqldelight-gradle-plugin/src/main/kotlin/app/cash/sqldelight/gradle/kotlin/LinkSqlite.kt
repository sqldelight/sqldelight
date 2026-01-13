package app.cash.sqldelight.gradle.kotlin

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal fun KotlinMultiplatformExtension.linkSqliteIfEnabled(linkSqlite: Provider<Boolean>) {
  targets.configureEach { target ->
    if (target is KotlinNativeTarget) {
      target.binaries.configureEach { binary ->
        // Defer reading the property until the binary is realized
        if (linkSqlite.get()) {
          binary.linkerOpts("-lsqlite3")
        }
      }
    }
  }
}
