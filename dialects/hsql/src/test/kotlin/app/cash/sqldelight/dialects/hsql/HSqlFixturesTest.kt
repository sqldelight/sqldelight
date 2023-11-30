package app.cash.sqldelight.dialects.hsql

import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class HSqlFixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "TEXT" to "VARCHAR(8)",
    "AUTOINCREMENT" to "AUTO_INCREMENT",
    "?1" to ":one",
    "?2" to ":two",
  )

  override fun setupDialect() {
    HsqlDialect().setup()
  }

  companion object {
    @Suppress("unused")
    // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = HsqlTestFixtures.fixtures + ansiFixtures
  }
}
