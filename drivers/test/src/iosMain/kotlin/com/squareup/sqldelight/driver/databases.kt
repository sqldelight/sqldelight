package com.squareup.sqldelight.driver

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.NativeDatabaseManager
import com.squareup.sqldelight.db.SqlDatabase

actual fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase {
  val database = SQLiterHelper(NativeDatabaseManager(
      "",
      DatabaseConfiguration("blah", 1, {})
  ))
  schema.create(database.getConnection())
  return database
}
