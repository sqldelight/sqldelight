package app.cash.sqldelight.dialects.mysql

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import com.alecstrong.sql.psi.test.fixtures.toParameter
import java.io.File

object MySqlTestFixtures {
  val fixtures = loadFolderFromResources("fixtures_mysql", File("build")).toParameter()
}
