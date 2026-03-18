package app.cash.sqldelight.dialects.hsql

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import com.alecstrong.sql.psi.test.fixtures.toParameter
import java.io.File

object HsqlTestFixtures {
  val fixtures = loadFolderFromResources("fixtures_hsql", File("build")).toParameter()
}
