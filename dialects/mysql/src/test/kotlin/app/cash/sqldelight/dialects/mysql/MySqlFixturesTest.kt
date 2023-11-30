package app.cash.sqldelight.dialects.mysql

import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class MySqlFixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "TEXT" to "VARCHAR(8)",
    "AUTOINCREMENT" to "AUTO_INCREMENT",
    "?1" to ":one",
    "?2" to ":two",
    "==" to "=",
    "'(', ')', ',', '.', <binary like operator real>, BETWEEN or IN expected, got ','"
      to "'(', ')', ',', '.', <binary like operator real>, <json binary operator real>, BETWEEN or IN expected, got ','",
  )

  override fun setupDialect() {
    MySqlDialect().setup()
  }

  companion object {
    @Suppress("unused")
    // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = MySqlTestFixtures.fixtures + ansiFixtures
  }
}
