package app.cash.sqldelight.gradle.android

import app.cash.sqldelight.VERSION
import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

internal fun Project.packageName(): String {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  return androidExtension.namespace ?: throw GradleException(
    """
    |SqlDelight requires a package name to be set. This can be done via the android namespace:
    |
    |android {
    |  namespace "com.example.mypackage"
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

internal fun Project.sqliteVersion(): String? {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  val minSdk = androidExtension.defaultConfig.minSdk ?: return null
  if (minSdk >= 31) return "app.cash.sqldelight:sqlite-3-30-dialect:$VERSION"
  if (minSdk >= 30) return "app.cash.sqldelight:sqlite-3-25-dialect:$VERSION"
  return "app.cash.sqldelight:sqlite-3-18-dialect:$VERSION"
}
