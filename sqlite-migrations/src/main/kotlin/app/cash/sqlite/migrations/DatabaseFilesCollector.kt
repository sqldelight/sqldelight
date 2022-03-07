package app.cash.sqlite.migrations

import java.io.File

fun Sequence<File>.findDatabaseFiles(): Sequence<File> = flatMap { it.walk() }.filter { entry ->
  entry.isFile && entry.name.endsWith(".db")
}
