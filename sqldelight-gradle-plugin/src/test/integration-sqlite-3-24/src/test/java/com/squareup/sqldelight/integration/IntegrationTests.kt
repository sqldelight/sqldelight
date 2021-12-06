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

  @Test fun upsertNoConflict() {
    // ?1 is the only arg
    personQueries.performUpsert(5, "Bo", "Jangles")

    assertThat(personQueries.selectAll().executeAsList())
      .containsExactly(
        Person(1, "Alec", "Strong"),
        Person(2, "Matt", "Precious"),
        Person(3, "Jake", "Wharton"),
        Person(4, "Bob", "Bob"),
        Person(5, "Bo", "Jangles")
      )
  }

  @Test fun upsertConflict() {
    // ?1 is the only arg
    personQueries.performUpsert(3, "James", "Mosley")

    assertThat(personQueries.selectAll().executeAsList())
      .containsExactly(
        Person(1, "Alec", "Strong"),
        Person(2, "Matt", "Precious"),
        Person(3, "James", "Mosley"),
        Person(4, "Bob", "Bob")
      )
  }
}
