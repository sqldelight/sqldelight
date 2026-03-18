package app.cash.sqldelight.dialects.sqlite_3_18

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object SqliteTestFixtures {
  val fixtures_sqlite_3_18 by loadFolderFromResources(target = File("build"))
  val fixtures_upsert_not_supported by loadFolderFromResources(target = File("build"))

  val fixtures = fixtures_sqlite_3_18 + fixtures_upsert_not_supported
}
