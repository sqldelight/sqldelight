package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.SqlDriver
import co.touchlab.sqliter.DatabaseFileContext
import com.squareup.sqldelight.driver.test.QueryTest

class NativeQueryTest : QueryTest() {
  override fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }
}
