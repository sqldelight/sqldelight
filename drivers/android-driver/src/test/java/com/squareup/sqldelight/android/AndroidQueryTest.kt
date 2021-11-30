package com.squareup.sqldelight.android

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.driver.test.QueryTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidQueryTest : QueryTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    return AndroidSqliteDriver(schema, getApplicationContext())
  }
}
