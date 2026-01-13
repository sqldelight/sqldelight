package app.cash.sqldelight.gradle.android

import app.cash.sqldelight.VERSION
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal fun Project.packageNameProvider(): Provider<String> = provider {
  val androidExtension = extensions.getByType(CommonExtension::class.java)
  androidExtension.namespace ?: throw GradleException(
    """
    |SqlDelight requires a package name to be set. This can be done via the android namespace:
    |
    |android {
    |  namespace = "com.example.mypackage"
    |}
    |
    |or the sqldelight configuration:
    |
    |sqldelight {
    |  MyDatabase {
    |    packageName = "com.example.mypackage"
    |  }
    |}
    """.trimMargin(),
  )
}

internal fun Project.sqliteVersionProvider(): Provider<String> = provider {
  val androidExtension = extensions.findByType(CommonExtension::class.java)
  val minSdk = androidExtension?.defaultConfig?.minSdk ?: return@provider "app.cash.sqldelight:sqlite-3-18-dialect:$VERSION"

  // Mapping available at https://developer.android.com/reference/android/database/sqlite/package-summary.
  when {
    minSdk >= 34 -> "app.cash.sqldelight:sqlite-3-38-dialect:$VERSION"
    minSdk >= 31 -> "app.cash.sqldelight:sqlite-3-30-dialect:$VERSION"
    minSdk >= 30 -> "app.cash.sqldelight:sqlite-3-25-dialect:$VERSION"
    else -> "app.cash.sqldelight:sqlite-3-18-dialect:$VERSION"
  }
}
