package com.squareup.sqldelight.driver.sqlite

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.driver.test.TransacterTest
import com.squareup.sqldelight.jdbc.driver.JdbcDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import java.sql.DriverManager
import java.util.Properties

class SqliteTransacterTest : TransacterTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    val database = JdbcDriver(DriverManager.getConnection(IN_MEMORY, Properties()))
    schema.create(database)
    return database
  }
}
