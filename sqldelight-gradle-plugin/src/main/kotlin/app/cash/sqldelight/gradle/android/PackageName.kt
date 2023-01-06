package app.cash.sqldelight.gradle.android

import app.cash.sqldelight.VERSION
import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

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

internal fun Project.sqliteVersion(): Dependency? {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  val minSdk = androidExtension.defaultConfig.minSdk ?: return null
  return dependencies.create(
    if (minSdk >= 31) "app.cash.sqldelight:sqlite-3-30-dialect:$VERSION" else if (minSdk >= 30) "app.cash.sqldelight:sqlite-3-25-dialect:$VERSION"
    else "app.cash.sqldelight:sqlite-3-18-dialect:$VERSION",
  )
}
