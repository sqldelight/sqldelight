package com.squareup.sqldelight.kmp

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.kmp.KmpSqliteDriver
import com.squareup.sqldelight.driver.test.QueryTest

class KmpQueryTest : QueryTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return KmpSqliteDriver(BundledSQLiteDriver(), "", schema)
  }
}
