package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test

class DebugIntegrationTests {
  private lateinit var queryWrapper: QueryWrapper
  private lateinit var debugQueries: DebugQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + "test.db")
    QueryWrapper.Schema.create(database)

    queryWrapper = QueryWrapper(database)
    debugQueries = queryWrapper.debugQueries
  }

  @After fun after() {
    File("test.db").delete()
  }

  @Test fun debugTable() {
    assertThat(debugQueries.selectAll().executeAsOne()).isEqualTo("debug")
  }
}
