package com.example

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import kotlin.Long

public class TestQueries(
  driver: SqlDriver,
  private val testAdapter: Test.Adapter,
) : TransacterImpl(driver) {
  /**
   * @return The number of rows updated.
   */
  public fun insert(test: Test): QueryResult<Long> {
    val result = driver.execute(-1_904_748_490, """INSERT INTO test (lastModifiedAt) VALUES (?)""", 1) {
          check(this is JdbcPreparedStatement)
          var parameterIndex = 0
          bindObject(parameterIndex++, testAdapter.lastModifiedAtAdapter.encode(test.lastModifiedAt))
        }
    notifyQueries(-1_904_748_490) { emit ->
      emit("test")
    }
    return result
  }
}
