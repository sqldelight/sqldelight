package com.squareup.sqldelight.android

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.db.use
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.driver.android.AndroidStatement
import com.squareup.sqldelight.driver.test.DriverTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return AndroidSqliteDriver(schema, getApplicationContext())
  }

  private fun useSingleItemCacheDriver(block: (AndroidSqliteDriver) -> Unit) {
    AndroidSqliteDriver(schema, getApplicationContext(), cacheSize = 1).use(block)
  }

  @Test
  fun `cached statement can be reused`() {
    useSingleItemCacheDriver { driver ->
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
  }

  @Test
  fun `cached statement is evicted and closed`() {
    useSingleItemCacheDriver { driver ->
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
  }

  @Test
  fun `uncached statement is closed`() {
    useSingleItemCacheDriver { driver ->
      lateinit var bindable: AndroidStatement
      driver.execute(null, "SELECT * FROM test", 0) {
        bindable = this as AndroidStatement
      }

      try {
        bindable.execute()
        throw AssertionError("Expected an IllegalStateException (attempt to re-open an already-closed object)")
      } catch (ignored: IllegalStateException) {
      }
    }
  }

  @Test
  fun `uses no backup directory`() {
    val factory = AssertableSupportSQLiteOpenHelperFactory()
    AndroidSqliteDriver(
      schema = schema,
      context = getApplicationContext(),
      factory = factory,
      name = "name",
      useNoBackupDirectory = true,
    ).use {
      assertTrue(factory.lastConfiguration.useNoBackupDirectory)
      assertSame("name", factory.lastConfiguration.name)
    }
  }

  @Test
  fun `uses backup directory`() {
    val factory = AssertableSupportSQLiteOpenHelperFactory()
    AndroidSqliteDriver(
      schema = schema,
      context = getApplicationContext(),
      factory = factory,
      useNoBackupDirectory = false,
    ).use {
      assertFalse(factory.lastConfiguration.useNoBackupDirectory)
    }
  }

  @Test
  fun `using a custom callback works`() {
    AndroidSqliteDriver(
      schema = schema,
      context = getApplicationContext(),
      callback = object : AndroidSqliteDriver.Callback(schema) {
        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
          db.execSQL("PRAGMA foreign_keys=ON;")
        }
      },
    ).close()
  }
}
