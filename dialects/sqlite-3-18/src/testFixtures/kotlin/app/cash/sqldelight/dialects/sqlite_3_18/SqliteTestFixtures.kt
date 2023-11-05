package app.cash.sqldelight.dialects.sqlite_3_18

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object SqliteTestFixtures {
    init {
        loadFolderFromResources("fixtures_sqlite_3_18", File("build"))
        loadFolderFromResources("fixtures_upsert_not_supported", File("build"))
    }

    val fixtures_sqlite_3_18 =
        File("build/fixtures_sqlite_3_18").listFiles()
            ?.filter { it.isDirectory }
            ?.map { arrayOf(it.name, it) } ?: emptyList()
    val fixtures_upsert_not_supported =
        File("build/fixtures_upsert_not_supported").listFiles()
            ?.filter { it.isDirectory }
            ?.map { arrayOf(it.name, it) } ?: emptyList()
  val fixtures = fixtures_sqlite_3_18 + fixtures_upsert_not_supported
}
