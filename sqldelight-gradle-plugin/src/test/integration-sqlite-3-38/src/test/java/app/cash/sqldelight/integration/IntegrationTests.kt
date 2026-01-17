package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private lateinit var jsonQueries: JsonQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(IN_MEMORY)
    QueryWrapper.Schema.create(database)

    val queryWrapper = QueryWrapper(database)
    jsonQueries = queryWrapper.jsonQueries
  }

  @Test fun selectByAlpha() {
    jsonQueries.insert("""{"alpha" : "abc"}""")
    jsonQueries.insert("{}")
    with(jsonQueries.selectAlpha().executeAsList()) {
      assertThat(first().a).isEqualTo(""""abc"""")
      assertThat(first().aa).isEqualTo("abc")
      assertThat(last().a).isNull()
      assertThat(last().a).isNull()
    }
  }

  @Test fun selectByArgs() {
    jsonQueries.insert("""{"alpha" : "abc"}""")
    jsonQueries.insert("{}")
    with(jsonQueries.selectBindArgs("alpha", "alpha").executeAsList()) {
      assertThat(first().a).isEqualTo(""""abc"""")
      assertThat(first().aa).isEqualTo("abc")
      assertThat(last().a).isNull()
      assertThat(last().a).isNull()
    }
  }
}
