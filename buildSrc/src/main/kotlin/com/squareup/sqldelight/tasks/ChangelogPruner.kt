package com.squareup.sqldelight.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

private val versionRegex = Regex("Version ([0-9]*)\\.([0-9]*)\\.([0-9]*) .*")

open class ChangelogPruner : DefaultTask() {
  @get:InputFile lateinit var changelog: File
  @get:OutputFile lateinit var output: File

  @TaskAction
  fun execute() {
    if (!changelog.exists()) throw IllegalArgumentException(
        "Need a CHANGELOG.md to write plugin.xml changelog"
    )

    class Version(val major: String, val minor: String, val patch: String)

    var currentVersion: Version? = null

    var lines = ""

    for (line in changelog.readLines().drop(2)) {
      val versionMatch = versionRegex.matchEntire(line)

      if (versionMatch != null) {
        val newVersion = Version(
            major = versionMatch.groups[1]!!.value,
            minor = versionMatch.groups[2]!!.value,
            patch = versionMatch.groups[3]!!.value
        )

        if (currentVersion != null &&
            (newVersion.major != currentVersion.major ||
                newVersion.minor != currentVersion.minor)
        ) break

        currentVersion = newVersion
      }
      lines += "$line\n"
    }

    output.parentFile.mkdirs()
    output.writeText(lines)
  }
}
