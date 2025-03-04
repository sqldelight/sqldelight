package com.example

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import kotlin.Long

public class DataQueries(
  driver: SqlDriver,
  private val testAdapter: Test.Adapter,
) : TransacterImpl(driver) {
  /**
   * @return The number of rows updated.
   */
  public fun insertWhole(test: Test): QueryResult<Long> {
    val result = driver.execute(-2_118_611_703, """
        |INSERT INTO test (third, second)
        |VALUES (?, ?)
        """.trimMargin(), 2) {
          check(this is JdbcPreparedStatement)
          bindString(0, test.third)
          bindString(1, test.second?.let { testAdapter.secondAdapter.encode(it) })
        }
    notifyQueries(-2_118_611_703) { emit ->
      emit("test")
    }
    return result
  }
}
