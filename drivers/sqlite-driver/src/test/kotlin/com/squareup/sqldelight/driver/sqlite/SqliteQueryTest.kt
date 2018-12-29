package com.squareup.sqldelight.driver.sqlite

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.driver.test.QueryTest
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper

class SqliteQueryTest: QueryTest() {
  override fun setupDatabase(schema: Schema): SqlDatabase {
    val database = SqliteJdbcOpenHelper()
    schema.create(database.getConnection())
    return database
  }
}