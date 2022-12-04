package app.cash.sqldelight.snapshotting.utils

import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.writeText

actual fun writeSnapshot(path: String, content: String) {
  val asPath = Path(path)
  Files.createDirectories(asPath)
  asPath.resolve("snapshot.txt").writeText(content)
}