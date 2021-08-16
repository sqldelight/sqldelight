package com.squareup.sqlite.migrations

import java.io.File

fun Sequence<File>.findDatabaseFiles(): Sequence<File> = flatMap(File::walk).filter { entry ->
  entry.isFile && entry.name.endsWith(".db")
}
