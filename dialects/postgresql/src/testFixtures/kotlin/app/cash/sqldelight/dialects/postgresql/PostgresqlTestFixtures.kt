package app.cash.sqldelight.dialects.postgresql

import java.io.File
import java.util.Enumeration
import java.util.jar.JarEntry
import java.util.jar.JarFile

object PostgresqlTestFixtures {
  // https://github.com/AlecKazakova/sql-psi/pull/552
  init {
    val path = "fixtures_postgresql"
    val jarFile = File(javaClass.protectionDomain.codeSource.location.path)
    val parentFile = File("build")
    File(parentFile, path).apply { if (exists()) deleteRecursively() }

    assert(jarFile.isFile)

    val jar = JarFile(jarFile)
    val entries: Enumeration<JarEntry> = jar.entries() // gives ALL entries in jar
    while (entries.hasMoreElements()) {
      val entry = entries.nextElement()
      val name: String = entry.name
      if (name.startsWith("$path/")) { // filter according to the path
        if (entry.isDirectory) {
          File(parentFile, entry.name).mkdir()
        } else {
          File(parentFile, entry.name).apply {
            createNewFile()
            jar.getInputStream(entry).use {
              it.copyTo(outputStream())
            }
          }
        }
      }
    }
    jar.close()
  }

  val fixtures = File("build/fixtures_postgresql").listFiles()
    .filter { it.isDirectory }
    .map { arrayOf(it.name, it) }
}
