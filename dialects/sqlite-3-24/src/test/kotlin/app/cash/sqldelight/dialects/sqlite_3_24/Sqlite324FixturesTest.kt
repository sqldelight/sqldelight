package app.cash.sqldelight.dialects.sqlite_3_24

import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTestFixtures as Sqlite_3_18SqliteTestFixtures
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class Sqlite324FixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "ORDER or WHERE expected" to "ORDER, WHERE or WINDOW expected",
  )

  override fun setupDialect() {
    SqliteDialect().setup()
  }

  companion object {
    @Suppress("unused")
    // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = Sqlite_3_18SqliteTestFixtures.fixtures_sqlite_3_18 + SqliteTestFixtures.fixtures + ansiFixtures
  }
}
