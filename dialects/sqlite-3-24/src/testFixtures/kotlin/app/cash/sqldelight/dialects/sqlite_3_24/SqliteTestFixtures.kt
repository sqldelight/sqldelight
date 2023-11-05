package app.cash.sqldelight.dialects.sqlite_3_24

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object SqliteTestFixtures {
  init {
    loadFolderFromResources("fixtures_sqlite_3_24", File("build"))
  }

  val fixtures = File("build/fixtures_sqlite_3_24").listFiles()
    ?.filter { it.isDirectory }
    ?.map { arrayOf(it.name, it) } ?: emptyList()
}
