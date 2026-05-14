package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private lateinit var strictQueries: StrictQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(IN_MEMORY)
    QueryWrapper.Schema.create(database)

    val queryWrapper = QueryWrapper(database)
    strictQueries = queryWrapper.strictQueries
  }

  @Test fun strictPrimaryKeyNotnullable() {
    val id: Long = strictQueries.selectTable1().executeAsOne()
    assertThat(id).isEqualTo(1)
  }

  @Test fun strictCompositePrimaryKeyNotnullable() {
    val table2 = strictQueries.selectTable2().executeAsOne()
    val id: Long = table2.id
    val txt: String = table2.txt
    assertThat(id).isEqualTo(99)
    assertThat(txt).isEqualTo("Some Text")
  }
}
