package com.squareup.sqldelight.integration

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import com.squareup.sqldelight.db.SqlDriver
import org.junit.Before
import org.junit.Test

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Assert.assertTrue

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
            Person.Impl(1, "Alec", "Strong"),
            Person.Impl(2, "Matt", "Precious"),
            Person.Impl(3, "Jake", "Wharton"),
            Person.Impl(4, "Bob", "Bob"),
            Person.Impl(5, "Bo", "Jangles")
        )
  }

  @Test fun upsertConflict() {
    // ?1 is the only arg
    personQueries.performUpsert(3, "James", "Mosley")

    assertThat(personQueries.selectAll().executeAsList())
        .containsExactly(
            Person.Impl(1, "Alec", "Strong"),
            Person.Impl(2, "Matt", "Precious"),
            Person.Impl(3, "James", "Mosley"),
            Person.Impl(4, "Bob", "Bob")
        )
  }
}
