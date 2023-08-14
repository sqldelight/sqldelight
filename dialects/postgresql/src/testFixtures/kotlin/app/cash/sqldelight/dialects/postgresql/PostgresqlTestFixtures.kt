package app.cash.sqldelight.dialects.postgresql

import java.io.File

object PostgresqlTestFixtures {
  val fixtures = File("build/fixtures_postgresql").listFiles()
    .filter { it.isDirectory }
    .map { arrayOf(it.name, it) }
}