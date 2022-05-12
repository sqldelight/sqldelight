package app.cash.sqldelight.dialects.sqlite_3_18

import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class Sqlite318FixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override fun setupDialect() {
    SqliteDialect().setup()
  }

  companion object {
    private val fixtures = arrayOf("src/test/fixtures_upsert_not_supported", "src/test/fixtures_sqlite_3_18")

    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic fun parameters() = fixtures.flatMap { fixtureFolder ->
      File(fixtureFolder).listFiles()!!
        .filter { it.isDirectory }
        .map { arrayOf(it.name, it) }
    } + ansiFixtures
  }
}
