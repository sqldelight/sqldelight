package com.squareup.sqldelight.driver

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.driver.DriverTest
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper

actual fun setupDatabase(schema: Schema): SqlDatabase {
  val database = SqliteJdbcOpenHelper()
  schema.create(database.getConnection())
  return database
}
