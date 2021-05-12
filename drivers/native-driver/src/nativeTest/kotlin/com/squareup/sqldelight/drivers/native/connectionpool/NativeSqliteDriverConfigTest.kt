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
  fun limitReaderConnectionsForMemoryDb() {
    assertEquals(1, (createDriver(DbType.InMemorySingle) as NativeSqliteDriver).readerPool.capacity)
    assertEquals(1, (createDriver(DbType.InMemoryShared) as NativeSqliteDriver).readerPool.capacity)
    assertEquals(4, (createDriver(DbType.RegularWal) as NativeSqliteDriver).readerPool.capacity)
    assertEquals(4, (createDriver(DbType.RegularDelete) as NativeSqliteDriver).readerPool.capacity)
  }
}
