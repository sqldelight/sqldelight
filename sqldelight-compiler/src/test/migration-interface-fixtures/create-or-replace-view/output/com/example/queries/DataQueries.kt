package com.example.queries

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcCursor
import com.example.migrations.V_test
import java.math.BigDecimal
import kotlin.Any
import kotlin.Int
import kotlin.String

public class DataQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> select(mapper: (
    a: String?,
    b: Int?,
    c: BigDecimal?,
  ) -> T): Query<T> = Query(879_500_825, arrayOf("test"), driver, "Data.sq", "select", """
  |SELECT a, b, c
  |FROM v_test
  """.trimMargin()) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getString(0),
      cursor.getInt(1),
      cursor.getBigDecimal(2)
    )
  }

  public fun select(): Query<V_test> = select { a, b, c ->
    V_test(
      a,
      b,
      c
    )
  }
}
