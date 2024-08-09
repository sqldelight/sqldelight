package com.example

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcCursor
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import kotlin.Any
import kotlin.String

public class DataQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun select(): Query<String> = Query(-1_042_942_063, arrayOf("test3"), driver, "Data.sq",
      "select", """
  |SELECT test3.alpha
  |FROM test3
  """.trimMargin()) { cursor ->
    check(cursor is JdbcCursor)
    cursor.getString(0)!!
  }

  public fun insert(test3: Test3): ExecutableQuery<String> = InsertQuery(test3) { cursor ->
    check(cursor is JdbcCursor)
    cursor.getString(0)!!
  }

  private inner class InsertQuery<out T : Any>(
    public val test3: Test3,
    mapper: (SqlCursor) -> T,
  ) : ExecutableQuery<T>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_320_712_882,
        """INSERT INTO test3 (alpha) VALUES (?) RETURNING test3.alpha""", mapper, 1) {
      check(this is JdbcPreparedStatement)
      bindString(0, test3.alpha)
    }

    override fun toString(): String = "Data.sq:insert"
  }
}
