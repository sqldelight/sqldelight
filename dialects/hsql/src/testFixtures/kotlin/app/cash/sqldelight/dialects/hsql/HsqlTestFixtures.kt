package app.cash.sqldelight.dialects.hsql

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object HsqlTestFixtures {
  init {
    loadFolderFromResources("fixtures_hsql", File("build"))
  }

  val fixtures = File("build/fixtures_hsql").listFiles()
    ?.filter { it.isDirectory }
    ?.map { arrayOf(it.name, it) } ?: emptyList()
}
