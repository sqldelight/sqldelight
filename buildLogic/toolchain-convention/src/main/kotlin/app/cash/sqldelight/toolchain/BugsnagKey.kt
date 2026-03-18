package app.cash.sqldelight.toolchain

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class BugsnagKey : DefaultTask() {
  @get:Input
  val bugsnagKey: Provider<String> = project.providers.gradleProperty("SQLDELIGHT_BUGSNAG_KEY").orElse("")

  @get:OutputDirectory
  val outputDir: Provider<Directory> = project.layout.buildDirectory.dir("gen")

  @TaskAction
  fun writeBugsnagKey() {
    val outputDir = outputDir.get().asFile
    val packageFile = File(outputDir, "app/cash/sqldelight/intellij")
    packageFile.mkdirs()
    val versionFile = File(packageFile, "Bugsnag.kt")
    versionFile.writeText(
      """// Generated file. Do not edit!
package app.cash.sqldelight.intellij

internal val BUGSNAG_KEY = "${bugsnagKey.get()}"
""",
    )
  }
}
