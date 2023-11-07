package app.cash.sqldelight.dialects.sqlite_3_30

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object SqliteTestFixtures {
  init {
    loadFolderFromResources("fixtures_sqlite_3_30", File("build"))
  }

  val fixtures = File("build/fixtures_sqlite_3_30").listFiles()
    ?.filter { it.isDirectory }
    ?.map { arrayOf(it.name, it) } ?: emptyList()
}
