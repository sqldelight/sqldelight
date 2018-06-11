package com.squareup.sqldelight.intellij.migrations

import java.io.File
import java.io.FileWriter

object MigrationScriptsWriter {
  fun writeMigrationScripts(path: String, scripts: List<String>) {
    val file = File(path)
    if (scripts.isNotEmpty()) {
      FileWriter(file).use { writer ->
        scripts.forEach {
          writer.appendln(it)
        }
      }
    } else {
      if (file.exists()) file.delete()
    }
  }
}
