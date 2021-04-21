package com.squareup.sqldelight.drivers.native.connectionpool

import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Testing driver config impact on internals. This is basically verifying that connection max config is set properly
 * internally.
 */
class NativeSqliteDriverConfigTest : BaseConcurrencyTest() {
  @Test
  fun inMemoryMaxOneConnection() {
    assertEquals(1, (createDriver(DbType.InMemorySingle) as NativeSqliteDriver)._maxTransactionConnections)
    assertEquals(1, (createDriver(DbType.InMemorySingle) as NativeSqliteDriver)._maxReaderConnections)
    assertEquals(1, (createDriver(DbType.InMemoryShared) as NativeSqliteDriver)._maxTransactionConnections)
    assertEquals(1, (createDriver(DbType.InMemoryShared) as NativeSqliteDriver)._maxReaderConnections)
  }

  @Test
  fun walMultipleConnection() {
    assertEquals(4, (createDriver(DbType.RegularWal) as NativeSqliteDriver)._maxTransactionConnections)
    assertEquals(4, (createDriver(DbType.RegularWal) as NativeSqliteDriver)._maxReaderConnections)

  }

  @Test
  fun delMultipleConnection() {
    assertEquals(1, (createDriver(DbType.RegularDelete) as NativeSqliteDriver)._maxTransactionConnections)
    assertEquals(4, (createDriver(DbType.RegularDelete) as NativeSqliteDriver)._maxReaderConnections)
  }
}