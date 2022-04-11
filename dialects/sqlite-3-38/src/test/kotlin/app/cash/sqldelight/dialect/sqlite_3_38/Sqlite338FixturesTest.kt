package app.cash.sqldelight.dialect.sqlite_3_38

import app.cash.sqldelight.dialects.sqlite_3_38.SqliteDialect
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class Sqlite338FixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "ORDER or WHERE expected" to "ORDER, WHERE or WINDOW expected"
  )

  override fun setupDialect() {
    SqliteDialect().setup()
  }

  companion object {
    private val fixtures = arrayOf("../sqlite-3-18/src/test/fixtures_sqlite_3_18", "../sqlite-3-24/src/test/fixtures_sqlite_3_24", "../sqlite-3-25/src/test/fixtures_sqlite_3_25", "../sqlite-3-30/src/test/fixtures_sqlite_3_30", "../sqlite-3-35/src/test/fixtures_sqlite_3_35", "src/test/fixtures_sqlite_3_38")

    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic fun parameters() = fixtures.flatMap { fixtureFolder ->
      File(fixtureFolder).listFiles()!!
        .filter { it.isDirectory }
        .map { arrayOf(it.name, it) }
    } + ansiFixtures
  }
}
