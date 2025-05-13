package com.example.queries

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcCursor
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import com.example.migrations.Test
import kotlin.Any
import kotlin.Int
import kotlin.String

public class DataQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> migrationSelect(mapper: (first: String, second: Int) -> T): Query<T> =
      Query(-1_568_224_787, arrayOf("test"), driver, "Data.sq", "migrationSelect", """
  |SELECT test.first, test.second
  |FROM test
  """.trimMargin()) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getInt(1)!!
    )
  }

  public fun migrationSelect(): Query<Test> = migrationSelect { first, second ->
    Test(
      first,
      second
    )
  }

  public fun <T : Any> migrationInsert(
    first: String,
    second: Int,
    mapper: (first: String, second: Int) -> T,
  ): ExecutableQuery<T> = MigrationInsertQuery(first, second) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getInt(1)!!
    )
  }

  public fun migrationInsert(first: String, second: Int): ExecutableQuery<Test> =
      migrationInsert(first, second) { first_, second_ ->
    Test(
      first_,
      second_
    )
  }

  public fun <T : Any> migrationDelete(first: String, mapper: (first: String, second: Int) -> T):
      ExecutableQuery<T> = MigrationDeleteQuery(first) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getInt(1)!!
    )
  }

  public fun migrationDelete(first: String): ExecutableQuery<Test> = migrationDelete(first) {
      first_, second ->
    Test(
      first_,
      second
    )
  }

  public fun <T : Any> migrationUpdate(first: String, mapper: (first: String, second: Int) -> T):
      ExecutableQuery<T> = MigrationUpdateQuery(first) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getString(0)!!,
      cursor.getInt(1)!!
    )
  }

  public fun migrationUpdate(first: String): ExecutableQuery<Test> = migrationUpdate(first) {
      first_, second ->
    Test(
      first_,
      second
    )
  }

  private inner class MigrationInsertQuery<out T : Any>(
    public val first: String,
    public val second: Int,
    mapper: (SqlCursor) -> T,
  ) : ExecutableQuery<T>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_845_995_606,
        """INSERT INTO test(first, second) VALUES (?, ?) RETURNING test.first, test.second""",
        mapper, 2) {
      check(this is JdbcPreparedStatement)
      bindString(0, first)
      bindInt(1, second)
    }

    override fun toString(): String = "Data.sq:migrationInsert"
  }

  private inner class MigrationDeleteQuery<out T : Any>(
    public val first: String,
    mapper: (SqlCursor) -> T,
  ) : ExecutableQuery<T>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_997_661_540,
        """DELETE FROM test WHERE first = ? RETURNING test.first, test.second""", mapper, 1) {
      check(this is JdbcPreparedStatement)
      bindString(0, first)
    }

    override fun toString(): String = "Data.sq:migrationDelete"
  }

  private inner class MigrationUpdateQuery<out T : Any>(
    public val first: String,
    mapper: (SqlCursor) -> T,
  ) : ExecutableQuery<T>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_501_049_414,
        """UPDATE test SET first = ? RETURNING test.first, test.second""", mapper, 1) {
      check(this is JdbcPreparedStatement)
      bindString(0, first)
    }

    override fun toString(): String = "Data.sq:migrationUpdate"
  }
}
