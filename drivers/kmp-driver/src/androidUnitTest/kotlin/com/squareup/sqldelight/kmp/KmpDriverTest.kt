package com.squareup.sqldelight.kmp

import android.content.Context
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.kmp.KmpSqliteDriver
import app.cash.sqldelight.driver.kmp.KmpStatement
import com.squareup.sqldelight.driver.test.DriverTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KmpDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val database = getApplicationContext<Context>().getDatabasePath("test.db")
    database.parentFile?.mkdirs()
    return KmpSqliteDriver(AndroidSQLiteDriver(), database.absolutePath, schema)
  }

  @Test
  fun `cached statement can be reused`() {
    val database = getApplicationContext<Context>().getDatabasePath("test.db")
    database.parentFile?.mkdirs()
    val driver = KmpSqliteDriver(AndroidSQLiteDriver(), database.absolutePath, schema, cacheSize = 1)
    lateinit var bindable: SqlPreparedStatement
    driver.executeQuery(1, "SELECT * FROM test", { QueryResult.Unit }, 0, { bindable = this })

    driver.executeQuery(
      1,
      "SELECT * FROM test",
      { QueryResult.Unit },
      0,
      {
        assertSame(bindable, this)
      },
    )
  }

  @Test
  fun `cached statement is evicted and closed`() {
    val database = getApplicationContext<Context>().getDatabasePath("test.db")
    database.parentFile?.mkdirs()
    val driver = KmpSqliteDriver(AndroidSQLiteDriver(), database.absolutePath, schema, cacheSize = 1)
    lateinit var bindable: SqlPreparedStatement
    driver.executeQuery(1, "SELECT * FROM test", { QueryResult.Unit }, 0, { bindable = this })

    driver.executeQuery(2, "SELECT * FROM test", { QueryResult.Unit }, 0)

    driver.executeQuery(
      1,
      "SELECT * FROM test",
      { QueryResult.Unit },
      0,
      {
        assertNotSame(bindable, this)
      },
    )
  }

  @Test
  fun `uncached statement is closed`() {
    val database = getApplicationContext<Context>().getDatabasePath("test.db")
    database.parentFile?.mkdirs()
    val driver = KmpSqliteDriver(AndroidSQLiteDriver(), database.absolutePath, schema, cacheSize = 1)
    lateinit var bindable: KmpStatement
    driver.execute(null, "SELECT * FROM test", 0) {
      bindable = this as KmpStatement
    }

    try {
      bindable.execute()
      throw AssertionError("Expected an IllegalStateException (attempt to re-open an already-closed object)")
    } catch (ignored: SQLiteException) {
    }
  }
}
