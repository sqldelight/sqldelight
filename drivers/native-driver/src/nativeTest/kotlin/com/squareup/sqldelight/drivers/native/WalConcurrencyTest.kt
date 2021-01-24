package com.squareup.sqldelight.drivers.native

import kotlin.test.BeforeTest

class WalConcurrencyTest: ConcurrencyTest() {
    @BeforeTest
    fun setup() {
        initDriver(DbType.RegularWal)
    }
}