package com.squareup.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class ReleaseIntegrationTests {
  private lateinit var queryWrapper: QueryWrapper
  private lateinit var releaseQueries: ReleaseQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + "test.db")
    QueryWrapper.Schema.create(database)

    queryWrapper = QueryWrapper(database)
    releaseQueries = queryWrapper.releaseQueries
  }

  @After fun after() {
    File("test.db").delete()
  }

  @Test fun releaseTable() {
    assertThat(releaseQueries.selectAll().executeAsOne()).isEqualTo("release")
  }
}
