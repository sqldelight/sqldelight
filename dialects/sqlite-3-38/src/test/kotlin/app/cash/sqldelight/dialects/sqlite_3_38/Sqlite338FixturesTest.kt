package app.cash.sqldelight.dialects.sqlite_3_38

import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTestFixtures as Sqlite_3_18SqliteTestFixtures
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteTestFixtures as Sqlite_3_24SqliteTestFixtures
import app.cash.sqldelight.dialects.sqlite_3_25.SqliteTestFixtures as Sqlite_3_25SqliteTestFixtures
import app.cash.sqldelight.dialects.sqlite_3_30.SqliteTestFixtures as Sqlite_3_30SqliteTestFixtures
import app.cash.sqldelight.dialects.sqlite_3_33.SqliteTestFixtures as Sqlite_3_33SqliteTestFixtures
import app.cash.sqldelight.dialects.sqlite_3_35.SqliteTestFixtures as Sqlite_3_35SqliteTestFixtures
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class Sqlite338FixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "ORDER or WHERE expected" to "ORDER, WHERE or WINDOW expected",
    "'(', ')', ',', '.', <binary like operator real>, BETWEEN or IN expected, got ','"
      to "'(', ')', ',', '.', <binary like operator real>, <json binary operator real>, BETWEEN or IN expected, got ','",
  )

  override fun setupDialect() {
    SqliteDialect().setup()
  }

  companion object {
    @Suppress("unused")
    // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() =
      Sqlite_3_18SqliteTestFixtures.fixtures_sqlite_3_18 +
        Sqlite_3_24SqliteTestFixtures.fixtures +
        Sqlite_3_25SqliteTestFixtures.fixtures +
        Sqlite_3_30SqliteTestFixtures.fixtures +
        Sqlite_3_33SqliteTestFixtures.fixtures +
        Sqlite_3_35SqliteTestFixtures.fixtures +
        SqliteTestFixtures.fixtures + ansiFixtures
  }
}
