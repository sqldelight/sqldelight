package com.squareup.sqldelight.kmp

import android.content.Context
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.kmp.KmpSqliteDriver
import com.squareup.sqldelight.driver.test.QueryTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KmpQueryTest : QueryTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val database = getApplicationContext<Context>().getDatabasePath("test.db")
    database.parentFile?.mkdirs()
    return KmpSqliteDriver(AndroidSQLiteDriver(), database.absolutePath, schema)
  }
}
