package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntegrationTests {
  @Test fun `running left join with named tables generates a valid select statement`() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TestDatabase.Schema.create(driver)
    val db = TestDatabase(driver)

    with(db.testQueries) {
      transaction {
        insertA(TableA("1", 123L))
        insertA(TableA("2", 456L))
        insertA(TableA("3", 789L))
        insertA(TableA("4", 1234L))

        insertB(TableB("1", 2345L))
        insertB(TableB("3", 6789L))
        insertB(TableB("5", 123456L))
      }
    }

    val expected = listOf(
      GetMatching(id = "1", value_ = 123L, id_ = "1", value__ = 2345L),
      GetMatching(id = "2", value_ = 456L, id_ = null, value__ = null),
      GetMatching(id = "3", value_ = 789L, id_ = "3", value__ = 6789L),
      GetMatching(id = "4", value_ = 1234L, id_ = null, value__ = null),
    )
    val actual = db.testQueries.getMatching().executeAsList()
    assertThat(actual).isEqualTo(expected)
  }
}
