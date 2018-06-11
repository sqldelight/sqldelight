package com.squareup.sqlite.migrations

import java.io.File

object DatabaseFilesCollector {
  inline fun forDatabaseFiles(sourceFolders: Iterable<File>, block: (File) -> Unit) {
    val folders = sourceFolders.toMutableList()
    while (folders.isNotEmpty()) {
      val folder = folders.removeAt(0)
      folder.listFiles()?.forEach { file ->
        if (file.name.endsWith(".db")) {
          block(file)
        } else if (file.isDirectory) {
          folders.add(file)
        }
      }
    }
  }
}
