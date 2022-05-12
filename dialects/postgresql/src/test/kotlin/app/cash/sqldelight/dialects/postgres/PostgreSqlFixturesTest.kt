package app.cash.sqldelight.dialects.postgres

import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import com.alecstrong.sql.psi.test.fixtures.FixturesTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
class PostgreSqlFixturesTest(name: String, fixtureRoot: File) : FixturesTest(name, fixtureRoot) {
  override val replaceRules = arrayOf(
    "INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT" to "SERIAL NOT NULL PRIMARY KEY",
    "AUTOINCREMENT" to "",
    "?1" to "?",
    "?2" to "?",
    "BLOB" to "TEXT",
  )

  override fun setupDialect() {
    PostgreSqlDialect().setup()
  }

  companion object {
    private val fixtures = arrayOf("src/test/fixtures_postgresql")

    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic fun parameters() = fixtures.flatMap { fixtureFolder ->
      File(fixtureFolder).listFiles()!!
        .filter { it.isDirectory }
        .map { arrayOf(it.name, it) }
    } + ansiFixtures
  }
}
