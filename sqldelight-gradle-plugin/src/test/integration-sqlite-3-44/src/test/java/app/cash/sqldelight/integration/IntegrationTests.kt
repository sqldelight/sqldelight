package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private lateinit var aggregateQueries: AggregateQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(IN_MEMORY)
    QueryWrapper.Schema.create(database)

    val queryWrapper = QueryWrapper(database)
    aggregateQueries = queryWrapper.aggregateQueries
  }

  @Test fun selectGroupConcat() {
    val names = aggregateQueries.selectActiveNames().executeAsOne()
    assertThat(names).isEqualTo(SelectActiveNames("User One, User Three"))
  }
}
