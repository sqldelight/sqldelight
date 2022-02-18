package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseFileContext
import com.squareup.sqldelight.driver.test.QueryTest

class NativeQueryTest : QueryTest() {
  override fun setupDatabase(
    schema: SqlDriver.Schema<SqlPreparedStatement, SqlCursor>,
  ): SqlDriver<SqlPreparedStatement, SqlCursor> {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }
}
