package com.squareup.sqldelight.drivers.native

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeSqliteDriverTest: BaseConcurrencyTest() {
    @Test
    fun inMemoryMaxOneConnection() {
        assertEquals(1, (createDriver(DbType.InMemorySingle) as NativeSqliteDriver)._maxConcurrentConnections)
        assertEquals(1, (createDriver(DbType.InMemoryShared) as NativeSqliteDriver)._maxConcurrentConnections)
    }

    @Test
    fun diskMultipleConnection() {
        assertEquals(4, (createDriver(DbType.RegularWal) as NativeSqliteDriver)._maxConcurrentConnections)
        assertEquals(4, (createDriver(DbType.RegularDelete) as NativeSqliteDriver)._maxConcurrentConnections)
    }
}