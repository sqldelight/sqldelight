package com.squareup.sqldelight.android

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.driver.test.DriverTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidDriverTest : DriverTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    return AndroidSqliteDriver(schema, RuntimeEnvironment.application)
  }

  @Test
  fun `cached statement can be reused`() {
    val driver = AndroidSqliteDriver(schema, RuntimeEnvironment.application, cacheSize = 1)
    lateinit var bindable: SqlPreparedStatement
    driver.executeQuery(1, "SELECT * FROM test", 0) {
      bindable = this
    }

    driver.executeQuery(1, "SELECT * FROM test", 0) {
      assertSame(bindable, this)
    }
  }

  @Test
  fun `cached statement is evicted and closed`() {
    val driver = AndroidSqliteDriver(schema, RuntimeEnvironment.application, cacheSize = 1)
    lateinit var bindable: SqlPreparedStatement
    driver.executeQuery(1, "SELECT * FROM test", 0) {
      bindable = this
    }

    driver.executeQuery(2, "SELECT * FROM test", 0)

    driver.executeQuery(1, "SELECT * FROM test", 0) {
      assertNotSame(bindable, this)
    }
  }

  @Test
  fun `uncached statement is closed`() {
    val driver = AndroidSqliteDriver(schema, RuntimeEnvironment.application, cacheSize = 1)
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

  @Test
  fun `uses no backup directory`() {
    val factory = AssertableSupportSQLiteOpenHelperFactory()
    val driver = AndroidSqliteDriver(
      schema = schema,
      context = RuntimeEnvironment.application,
      factory = factory,
      name = "name",
      useNoBackupDirectory = true
    )

    assertTrue(factory.lastConfiguration.useNoBackupDirectory)
    assertSame("name", factory.lastConfiguration.name)
  }

  @Test
  fun `uses backup directory`() {
    val factory = AssertableSupportSQLiteOpenHelperFactory()
    val driver = AndroidSqliteDriver(
      schema = schema,
      context = RuntimeEnvironment.application,
      factory = factory,
      useNoBackupDirectory = false
    )

    assertFalse(factory.lastConfiguration.useNoBackupDirectory)
  }
}
