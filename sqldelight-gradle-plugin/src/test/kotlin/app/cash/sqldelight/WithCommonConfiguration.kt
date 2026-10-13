package app.cash.sqldelight

import java.io.File
import java.util.Properties
import org.gradle.testkit.runner.GradleRunner

// AGP 8 relies on Problems API internals such as org.gradle.api.problems.internal.InternalProblems,
// which Gradle 9.6 removed, so its tests must use Gradle 9.5.1.
// See https://docs.gradle.org/9.6.0/userguide/upgrading_version_9.html#agp_8x_incompatible
internal const val AGP_8_MAX_GRADLE_VERSION = "9.5.1"

internal fun GradleRunner.withCommonConfiguration(
  projectRoot: File,
  enableIsolatedProject: Boolean = true,
): GradleRunner {
  File(projectRoot, "gradle.properties").writeText(
    """
      |org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
      |android.useAndroidX=true
      |android.newDsl=true
      |org.gradle.unsafe.isolated-projects=$enableIsolatedProject
      |
    """.trimMargin(),
  )
  File(projectRoot, "local.properties").apply {
    if (!exists()) writeText("sdk.dir=${androidHome()}\n")
  }
  return withProjectDir(projectRoot).withTestKitDir(File("build/gradle-test-kit").absoluteFile)
}

private fun androidHome(): String {
  val env = System.getenv("ANDROID_HOME")
  if (env != null) {
    return env.withInvariantPathSeparators()
  }
  val localProp = File(File(System.getProperty("user.dir")).parentFile, "local.properties")
  if (localProp.exists()) {
    val prop = Properties()
    localProp.inputStream().use {
      prop.load(it)
    }
    val sdkHome = prop.getProperty("sdk.dir")
    if (sdkHome != null) {
      return sdkHome.withInvariantPathSeparators()
    }
  }
  throw IllegalStateException(
    "Missing 'ANDROID_HOME' environment variable or local.properties with 'sdk.dir'",
  )
}
