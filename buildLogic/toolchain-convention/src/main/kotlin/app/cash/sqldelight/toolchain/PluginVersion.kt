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
abstract class PluginVersion : DefaultTask() {
  @get:Input
  val gitHash: Provider<String> = project.providers.exec {
    it.commandLine("git", "rev-parse", "--short", "HEAD")
  }.standardOutput.asText

  @get:Input
  val version: String = project.version.toString()

  @get:OutputDirectory
  val outputDir: Provider<Directory> = project.layout.buildDirectory.dir("gen")

  @TaskAction
  fun writeVersionFile() {
    val outputDir = outputDir.get().asFile
    val packageFile = File(outputDir, "app/cash/sqldelight")
    packageFile.mkdirs()
    val versionFile = File(packageFile, "Version.kt")
    versionFile.writeText(
"""// Generated file. Do not edit!
package app.cash.sqldelight

val VERSION = "$version"
val GIT_SHA = "${gitHash.get().trim()}"
""",
    )
  }
}
