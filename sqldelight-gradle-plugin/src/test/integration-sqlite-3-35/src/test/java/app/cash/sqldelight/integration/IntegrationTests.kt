package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private lateinit var queryWrapper: QueryWrapper
  private lateinit var personQueries: PersonQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(IN_MEMORY)
    QueryWrapper.Schema.create(database)

    queryWrapper = QueryWrapper(database)
    personQueries = queryWrapper.personQueries
  }

  @Test fun returningInsert() {
    assertThat(database.personQueries.insertAndReturn(1, "Alec", "Strong").executeAsOne())
      .isEqualTo(
        Person(1, "Alec", "Strong")
      )
  }
}
