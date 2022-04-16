package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private val moshi = Moshi.Builder().build()

  private lateinit var queryWrapper: QueryWrapper
  private lateinit var jsonQueries: JsonTableQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.create(database)

    queryWrapper = QueryWrapper(database)
    jsonQueries = queryWrapper.jsonTableQueries
  }

  @Test fun jsonArray() {
    with(jsonQueries) {
      insertUser("user1", jsonPhones("704-555-5555", "705-555-5555"))
      insertUser("user2", jsonPhones("604-555-5555", "605-555-5555"))
      assertThat(byAreaCode(areaCode = "704").executeAsList()).containsExactly(
        "user1"
      )
    }
  }

  @Test fun jsonArrayOrLiteral() {
    with(jsonQueries) {
      insertUser("user1", jsonPhones("704-555-5555", "705-555-5555"))
      insertUser("user2", jsonPhones("604-555-5555", "605-555-5555"))
      insertUser("user3", "704-666-6666")
      assertThat(byAreaCode2(areaCode = "704").executeAsList()).containsExactly(
        "user1",
        "user3"
      )
    }
  }

  private fun jsonPhones(vararg phoneNumbers: String): String {
    val adapter =
      moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
    return adapter.toJson(phoneNumbers.toList())
  }
}
