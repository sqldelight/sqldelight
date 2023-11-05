package app.cash.sqldelight.dialects.postgresql

import java.io.File
import java.nio.file.FileSystems
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.div
import kotlin.io.path.toPath

object PostgresqlTestFixtures {
  init {
    loadFolderFromResources("fixtures_postgresql", File("build"))
  }

  // https://github.com/AlecKazakova/sql-psi/pull/552
  @OptIn(ExperimentalPathApi::class)
  private fun Any.loadFolderFromResources(path: String, target: File) {
    File(target, path).apply { if (exists()) deleteRecursively() }
    val resourcesUri = javaClass.getResource("/$path")?.toURI()
    requireNotNull(resourcesUri) {
      "/$path not found in resources"
    }
    when (resourcesUri.scheme) {
      "jar" -> FileSystems.newFileSystem(resourcesUri, emptyMap<String, Nothing>(), null).use {
        it.getPath("/$path").copyToRecursively(target.toPath() / path, overwrite = true, followLinks = false)
      }
      "file" -> resourcesUri.toPath().copyToRecursively(target.toPath() / path, overwrite = true, followLinks = false)
      else -> error("Unsupported scheme ${resourcesUri.scheme} of $resourcesUri")
    }
  }

  val fixtures = File("build/fixtures_postgresql").listFiles()
    .filter { it.isDirectory }
    .map { arrayOf(it.name, it) }
}
