package com.squareup.sqldelight.integration

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper

actual fun createSqlDatabase(): SqlDatabase {
  return SqliteJdbcOpenHelper()
}
