package app.cash.sqldelight.dialects.sqlite.json.module

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object SqliteJsonTestFixtures {
  init {
    loadFolderFromResources("fixtures_sqlite_json", File("build"))
  }

  val fixtures = File("build/fixtures_sqlite_json").listFiles()
    ?.filter { it.isDirectory }
    ?.map { arrayOf(it.name, it) } ?: emptyList()
}
