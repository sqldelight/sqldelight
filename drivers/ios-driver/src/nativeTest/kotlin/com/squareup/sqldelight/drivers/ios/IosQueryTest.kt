package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.DatabaseFileContext
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.driver.test.QueryTest

class IosQueryTest: QueryTest() {
  override fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }
}