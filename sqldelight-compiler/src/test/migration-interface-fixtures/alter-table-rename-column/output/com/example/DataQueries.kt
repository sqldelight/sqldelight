package com.example

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import kotlin.Unit

public class DataQueries(
  driver: SqlDriver,
  private val testAdapter: Test.Adapter,
) : TransacterImpl(driver) {
  public fun insertWhole(test: Test): Unit {
    driver.execute(-2118611703, """
        |INSERT INTO test
        |VALUES (?, ?)
        """.trimMargin(), listOf(25, 28)) {
          check(this is JdbcPreparedStatement)
          bindString(0, test.third)
          bindString(1, test.second?.let { testAdapter.secondAdapter.encode(it) })
        }
    notifyQueries(-2118611703) { emit ->
      emit("test")
    }
  }
}
