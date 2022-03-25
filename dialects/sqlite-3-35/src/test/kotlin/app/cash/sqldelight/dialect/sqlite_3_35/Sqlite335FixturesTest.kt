package app.cash.sqldelight.dialect.sqlite_3_35

import app.cash.sqldelight.dialects.sqlite_3_35.SqliteDialect
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class Sqlite335FixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "ORDER or WHERE expected" to "ORDER, WHERE or WINDOW expected"
  )

  override fun setupDialect() {
    SqliteDialect().setup()
  }

  companion object {
    private val fixtures = arrayOf(/*"src/test/fixtures_sqlite_3_18", "src/test/fixtures_sqlite_3_24", "src/test/fixtures_sqlite_3_25", "src/test/fixtures_sqlite_3_30",*/ "src/test/fixtures_sqlite_3_35")

    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic fun parameters() = fixtures.flatMap { fixtureFolder ->
      File(fixtureFolder).listFiles()!!
        .filter { it.isDirectory }
        .map { arrayOf(it.name, it) }
    } + ansiFixtures
  }
}
