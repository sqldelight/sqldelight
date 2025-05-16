package app.cash.sqldelight.dialects.sqlite.json.module

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import com.alecstrong.sql.psi.test.fixtures.toParameter
import java.io.File

object SqliteJsonTestFixtures {
  val fixtures = loadFolderFromResources("fixtures_sqlite_json", File("build")).toParameter()
}
