package app.cash.sqldelight.gradle.android

import app.cash.sqldelight.VERSION
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.manifest.parseManifest
import com.android.builder.errors.EvalIssueException
import com.android.builder.errors.IssueReporter
import org.gradle.api.Project
import java.util.function.BooleanSupplier

internal fun Project.packageName(): String {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  androidExtension.sourceSets
    .map { it.manifest.srcFile }
    .filter { it.exists() }
    .forEach {
      return parseManifest(
        file = it,
        manifestFileRequired = true,
        manifestParsingAllowed = BooleanSupplier { true },
        issueReporter = object : IssueReporter() {
          override fun hasIssue(type: Type) = false
          override fun reportIssue(type: Type, severity: Severity, exception: EvalIssueException) = throw exception
        }
      ).packageName!!
    }
  throw IllegalStateException("No source sets available")
}

internal fun Project.sqliteVersion(): String? {
  val androidExtension = extensions.getByType(BaseExtension::class.java)
  val minSdk = androidExtension.defaultConfig.minSdk ?: return null
  if (minSdk == 30) return "app.cash.sqldelight:sqlite-3-25-dialect:$VERSION"
  if (minSdk >= 31) return "app.cash.sqldelight:sqlite-3-30-dialect:$VERSION"
  return "app.cash.sqldelight:sqlite-3-18-dialect:$VERSION"
}
