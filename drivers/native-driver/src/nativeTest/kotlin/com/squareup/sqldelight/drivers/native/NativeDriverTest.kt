package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import co.touchlab.sqliter.DatabaseFileContext.deleteDatabase
import com.squareup.sqldelight.driver.test.DriverTest

class NativeDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema): SqlDriver {
    val name = "testdb"
    deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }
}

class NativeDriverMemoryTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema): SqlDriver {
    return inMemoryDriver(schema)
  }
}
