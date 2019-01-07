package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.NativeFileContext
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.driver.test.QueryTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class IosQueryTest: QueryTest() {
  override fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
    val name = "testdb"
    NativeFileContext.deleteDatabase(name)
    return NativeSqliteDriver(schema, name)
  }

  // TODO: https://github.com/JetBrains/kotlin-native/issues/2328

  @BeforeTest fun setup2() {
    super.setup()
  }

  @AfterTest fun tearDown2() {
    super.tearDown()
  }


  @Test fun executeAsOne2() {
    super.executeAsOne()
  }

  @Test fun executeAsOneTwoTimes2() {
    super.executeAsOneTwoTimes()
  }

  @Test fun executeAsOneThrowsNpeForNoRows2() {
    super.executeAsOneThrowsNpeForNoRows()
  }

  @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows2() {
    super.executeAsOneThrowsIllegalStateExceptionForManyRows()
  }

  @Test fun executeAsOneOrNull2() {
    super.executeAsOneOrNull()
  }

  @Test fun executeAsOneOrNullReturnsNullForNoRows2() {
    super.executeAsOneOrNullReturnsNullForNoRows()
  }

  @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows2() {
    super.executeAsOneOrNullThrowsIllegalStateExceptionForManyRows()
  }

  @Test fun executeAsList2() {
    super.executeAsList()
  }

  @Test fun executeAsListForNoRows2() {
    super.executeAsListForNoRows()
  }

  @Test fun notifyDataChangedNotifiesListeners2() {
    super.notifyDataChangedNotifiesListeners()
  }

  @Test fun removeListenerActuallyRemovesListener2() {
    super.removeListenerActuallyRemovesListener()
  }
}