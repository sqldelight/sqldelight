package app.cash.sqldelight.dialects.sqlite.json.module

import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class JsonModuleTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override fun setupDialect() {
    SqliteDialect().setup()
    JsonModule().setup()
  }

  companion object {
    @Suppress("unused")
    // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = SqliteJsonTestFixtures.fixtures + ansiFixtures
  }
}
