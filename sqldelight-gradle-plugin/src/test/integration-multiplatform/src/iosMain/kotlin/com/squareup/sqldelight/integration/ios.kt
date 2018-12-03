package com.squareup.sqldelight.integration

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.drivers.ios.SQLiterHelper
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.NativeFileContext.deleteDatabase
import co.touchlab.sqliter.DatabaseConfiguration

actual fun createSqlDatabase(): SqlDatabase {
  val configuration = DatabaseConfiguration("testdb", 1, { })
  deleteDatabase(configuration.name)
  return SQLiterHelper(createDatabaseManager(configuration))
}