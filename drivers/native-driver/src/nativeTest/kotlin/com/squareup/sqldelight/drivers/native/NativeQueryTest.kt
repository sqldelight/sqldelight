package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseFileContext
import com.squareup.sqldelight.driver.test.QueryTest

class NativeQueryTest : QueryTest() {
  override fun setupDatabase(schema: SqlSchema): SqlDriver {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }
}
