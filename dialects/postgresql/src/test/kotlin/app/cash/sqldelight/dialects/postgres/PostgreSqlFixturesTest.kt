package app.cash.sqldelight.dialects.postgres

import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.dialects.postgresql.PostgresqlTestFixtures
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import java.io.File
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class PostgreSqlFixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT" to "SERIAL NOT NULL PRIMARY KEY",
    "AUTOINCREMENT" to "",
    "?1" to "?",
    "?2" to "?",
    "BLOB" to "TEXT",
    "id TEXT GENERATED ALWAYS AS (2) UNIQUE NOT NULL" to "id TEXT GENERATED ALWAYS AS (2) STORED UNIQUE NOT NULL",
    "'(', ')', ',', '.', <binary like operator real>, BETWEEN or IN expected, got ','"
      to "'#-', '(', ')', ',', '.', <binary like operator real>, <jsona binary operator real>, <jsonb boolean operator real>, '@@', BETWEEN or IN expected, got ','",
  )

  override fun setupDialect() {
    PostgreSqlDialect().setup()
  }

  companion object {
    @Suppress("unused")
    // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic
    fun parameters() = PostgresqlTestFixtures.fixtures + ansiFixtures
  }
}
