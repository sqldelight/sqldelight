package com.example

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcCursor
import kotlin.Any
import kotlin.Int
import kotlin.String

public class DataQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectSingle(mapper: (first: Int, second: String?) -> T): Query<T> =
      Query(-79_317_191, arrayOf("TestSingle"), driver, "Data.sq", "selectSingle", """
  |SELECT TestSingle.first, TestSingle.second
  |FROM TestSingle
  """.trimMargin()) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getInt(0)!!,
      cursor.getString(1)
    )
  }

  public fun selectSingle(): Query<TestSingle> = selectSingle { first, second ->
    TestSingle(
      first,
      second
    )
  }

  public fun <T : Any> selectCompound(mapper: (first: Int, second: String) -> T): Query<T> =
      Query(-19_725_220, arrayOf("TestCompound"), driver, "Data.sq", "selectCompound", """
  |SELECT TestCompound.first, TestCompound.second
  |FROM TestCompound
  """.trimMargin()) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getInt(0)!!,
      cursor.getString(1)!!
    )
  }

  public fun selectCompound(): Query<TestCompound> = selectCompound { first, second ->
    TestCompound(
      first,
      second
    )
  }
}
