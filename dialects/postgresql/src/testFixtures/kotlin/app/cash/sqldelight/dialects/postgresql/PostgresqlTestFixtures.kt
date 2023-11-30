package app.cash.sqldelight.dialects.postgresql

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import com.alecstrong.sql.psi.test.fixtures.toParameter
import java.io.File

object PostgresqlTestFixtures {
  val fixtures = loadFolderFromResources("fixtures_postgresql", File("build")).toParameter()
}
