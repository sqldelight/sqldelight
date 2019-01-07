package com.squareup.sqldelight.driver.sqlite

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.driver.test.DriverTest
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

class SqliteDriverTest : DriverTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    val database = JdbcSqliteDriver()
    schema.create(database)
    return database
  }
}