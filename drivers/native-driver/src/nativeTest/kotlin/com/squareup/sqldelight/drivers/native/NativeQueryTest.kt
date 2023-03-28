package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import co.touchlab.sqliter.DatabaseFileContext
import com.squareup.sqldelight.driver.test.QueryTest

class NativeQueryTest : QueryTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }
}

class NativeQueryMemoryTest : QueryTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return inMemoryDriver(schema)
  }
}
