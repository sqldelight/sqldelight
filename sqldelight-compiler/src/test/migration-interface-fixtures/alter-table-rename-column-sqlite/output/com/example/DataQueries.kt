package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlin.Long
import kotlin.String

public class DataQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun select(): Query<String> = Query(-1_042_942_063, arrayOf("test"), driver, "Data.sq", "select", """
  |SELECT test.alpha
  |  FROM test
  """.trimMargin()) { cursor ->
    cursor.getString(0)!!
  }

  /**
   * @return The number of rows updated.
   */
  public fun insert(alpha: String): QueryResult<Long> {
    val result = driver.execute(-1_320_712_882, """INSERT INTO test (alpha) VALUES (?)""", 1) {
          var parameterIndex = 0
          bindString(parameterIndex++, alpha)
        }
    notifyQueries(-1_320_712_882) { emit ->
      emit("test")
    }
    return result
  }
}
