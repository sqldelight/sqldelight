package com.squareup.sqldelight.android

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlDriver.Schema
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.squareup.sqldelight.driver.test.QueryTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidQueryTest : QueryTest() {
  override fun setupDatabase(
    schema: Schema<SqlPreparedStatement, SqlCursor>
  ): SqlDriver<SqlPreparedStatement, SqlCursor> {
    return AndroidSqliteDriver(schema, getApplicationContext())
  }
}
