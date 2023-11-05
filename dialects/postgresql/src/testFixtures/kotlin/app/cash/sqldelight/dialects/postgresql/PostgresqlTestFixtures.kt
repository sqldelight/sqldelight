package app.cash.sqldelight.dialects.postgresql

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object PostgresqlTestFixtures {
  init {
    loadFolderFromResources("fixtures_postgresql", File("build"))
  }

  val fixtures = File("build/fixtures_postgresql").listFiles()
    ?.filter { it.isDirectory }
    ?.map { arrayOf(it.name, it) } ?: emptyList()
}
