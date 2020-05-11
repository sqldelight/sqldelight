package com.squareup.sqldelight.android

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.driver.test.QueryTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidQueryTest : QueryTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    return AndroidSqliteDriver(schema, RuntimeEnvironment.application)
  }
}
