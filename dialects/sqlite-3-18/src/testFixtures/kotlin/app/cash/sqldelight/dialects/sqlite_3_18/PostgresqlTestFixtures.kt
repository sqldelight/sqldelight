package app.cash.sqldelight.dialects.sqlite_3_18

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object SqliteTestFixtures {
  init {
    loadFolderFromResources("fixtures_sqlite_3_18", File("build"))
    loadFolderFromResources("fixtures_upsert_not_supported", File("build"))
  }

  val fixtures = listOf(
    "fixtures_sqlite_3_18",
    "fixtures_upsert_not_supported",
  ).flatMap { fixtureFolder ->
    File("build/$fixtureFolder").listFiles()
      ?.filter { it.isDirectory }
      ?.map { arrayOf(it.name, it) } ?: emptyList()
  }
}
