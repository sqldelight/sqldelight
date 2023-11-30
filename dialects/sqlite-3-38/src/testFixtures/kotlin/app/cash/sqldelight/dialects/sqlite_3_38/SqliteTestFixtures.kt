package app.cash.sqldelight.dialects.sqlite_3_38

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import com.alecstrong.sql.psi.test.fixtures.toParameter
import java.io.File

object SqliteTestFixtures {
  val fixtures = loadFolderFromResources("fixtures_sqlite_3_38", File("build")).toParameter()
}
