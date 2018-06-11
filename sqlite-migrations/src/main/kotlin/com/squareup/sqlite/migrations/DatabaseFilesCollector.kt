package com.squareup.sqlite.migrations

import java.io.File
import java.util.ArrayDeque

object DatabaseFilesCollector {
  inline fun forDatabaseFiles(sourceFolders: Iterable<File>, block: (File) -> Unit) {
    val folders = ArrayDeque<File>().apply { sourceFolders.forEach(this::addLast) }
    while (folders.isNotEmpty()) {
      val folder = folders.removeFirst()
      folder.listFiles()?.forEach { file ->
        if (file.name.endsWith(".db")) {
          block(file)
        } else if (file.isDirectory) {
          folders.addLast(file)
        }
      }
    }
  }
}
