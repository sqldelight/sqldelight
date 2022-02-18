package com.squareup.sqldelight.driver.sqlite

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlDriver.Schema
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.squareup.sqldelight.driver.test.TransacterTest

class SqliteTransacterTest : TransacterTest() {
  override fun setupDatabase(
    schema: Schema<SqlPreparedStatement, SqlCursor>,
  ): SqlDriver<SqlPreparedStatement, SqlCursor> {
    val database = JdbcSqliteDriver(IN_MEMORY)
    schema.create(database)
    return database
  }
}
