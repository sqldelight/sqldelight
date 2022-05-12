package com.squareup.sqldelight.driver.sqlite

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.squareup.sqldelight.driver.test.DriverTest

class SqliteDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema): SqlDriver {
    val database = JdbcSqliteDriver(IN_MEMORY)
    schema.create(database)
    return database
  }
}
