package app.cash.sqldelight.dialects.sqlite.json.module

import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class JsonModuleTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override fun setupDialect() {
    SqliteDialect().setup()
    JsonModule().setup()
  }

  companion object {
    private val fixtures = arrayOf("src/test/fixtures")

    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic fun parameters() = fixtures.flatMap { fixtureFolder ->
      File(fixtureFolder).listFiles()!!
        .filter { it.isDirectory }
        .map { arrayOf(it.name, it) }
    } + ansiFixtures
  }
}
