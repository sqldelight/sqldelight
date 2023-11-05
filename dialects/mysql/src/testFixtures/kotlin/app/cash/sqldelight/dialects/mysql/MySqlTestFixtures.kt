package app.cash.sqldelight.dialects.mysql

import com.alecstrong.sql.psi.test.fixtures.loadFolderFromResources
import java.io.File

object MySqlTestFixtures {
  init {
    loadFolderFromResources("fixtures_mysql", File("build"))
  }

  val fixtures = File("build/fixtures_mysql").listFiles()
    ?.filter { it.isDirectory }
    ?.map { arrayOf(it.name, it) } ?: emptyList()
}
