package com.squareup.sqldelight.driver.sqlite

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.driver.test.DriverTest
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY

class SqliteDriverTest : DriverTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    val database = JdbcSqliteDriver(IN_MEMORY)
    schema.create(database)
    return database
  }
}
