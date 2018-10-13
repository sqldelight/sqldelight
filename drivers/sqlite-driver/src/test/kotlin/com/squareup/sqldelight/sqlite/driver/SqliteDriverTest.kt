package com.squareup.sqldelight.sqlite.driver

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.driver.DriverTest

class SqliteDriverTest : DriverTest() {
  override fun setupDatabase(schema: Schema): SqlDatabase {
    val database = SqliteJdbcOpenHelper()
    schema.create(database.getConnection())
    return database
  }
}