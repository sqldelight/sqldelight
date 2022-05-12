package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public class DataQueries(
  private val driver: SqlDriver,
  private val testAdapter: Test.Adapter,
) : TransacterImpl(driver) {
  public fun <T : Any> migrationSelect(mapper: (first: String, second: List<Int>?) -> T): Query<T> =
      Query(-561113227, arrayOf("new_test"), driver, "Data.sq", "migrationSelect", """
  |SELECT *
  |FROM new_test
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)?.let { testAdapter.secondAdapter.decode(it) }
    )
  }

  public fun migrationSelect(): Query<New_test> = migrationSelect { first, second ->
    New_test(
      first,
      second
    )
  }
}
