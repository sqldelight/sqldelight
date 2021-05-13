package com.squareup.sqldelight.logs

import com.squareup.sqldelight.db.AfterVersion
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.migrateWithCallbacks
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqlDriverMigrationCallbackTest {

  @Test fun migrationCallbackInvokedOnCorrectVersion() {
    val schema = fakeSchema()

    var callbackInvoked = false

    schema.migrateWithCallbacks(
      driver = FakeSqlDriver(),
      oldVersion = 1,
      newVersion = 2,
      AfterVersion(1) { callbackInvoked = true }
    )

    assertTrue(callbackInvoked)
  }

  @Test fun migrationCallbacks() {
    val schema = fakeSchema()

    var callback1Invoked = false
    var callback2Invoked = false
    var callback3Invoked = false

    schema.migrateWithCallbacks(
      driver = FakeSqlDriver(),
      oldVersion = 0,
      newVersion = 4,
      AfterVersion(1) { callback1Invoked = true },
      AfterVersion(2) { callback2Invoked = true },
      AfterVersion(3) { callback3Invoked = true },
    )

    assertTrue(callback1Invoked)
    assertTrue(callback2Invoked)
    assertTrue(callback3Invoked)
  }

  @Test fun migrationCallbackLessThanOldVersionNotCalled() {
    val schema = fakeSchema()

    var callback1Invoked = false
    var callback2Invoked = false
    var callback3Invoked = false

    schema.migrateWithCallbacks(
      driver = FakeSqlDriver(),
      oldVersion = 2,
      newVersion = 4,
      AfterVersion(1) { callback1Invoked = true },
      AfterVersion(2) { callback2Invoked = true },
      AfterVersion(3) { callback3Invoked = true },
    )

    assertFalse(callback1Invoked)
    assertTrue(callback2Invoked)
    assertTrue(callback3Invoked)
  }

  @Test fun migrationCallbackGreaterThanNewVersionNotCalled() {
    val schema = fakeSchema()

    var callback1Invoked = false
    var callback2Invoked = false
    var callback3Invoked = false
    var callback4Invoked = false

    schema.migrateWithCallbacks(
      driver = FakeSqlDriver(),
      oldVersion = 0,
      newVersion = 4,
      AfterVersion(1) { callback1Invoked = true },
      AfterVersion(2) { callback2Invoked = true },
      AfterVersion(3) { callback3Invoked = true },
      AfterVersion(4) { callback4Invoked = true },
    )

    assertTrue(callback1Invoked)
    assertTrue(callback2Invoked)
    assertTrue(callback3Invoked)
    assertFalse(callback4Invoked)
  }

  private fun fakeSchema() = object : SqlDriver.Schema {
    override val version: Int = 1
    override fun create(driver: SqlDriver) = Unit
    override fun migrate(driver: SqlDriver, oldVersion: Int, newVersion: Int) = Unit
  }
}
