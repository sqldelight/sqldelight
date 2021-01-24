package com.squareup.sqldelight.drivers.native

import kotlin.test.BeforeTest

class DeleteConcurrencyTest: ConcurrencyTest() {
    @BeforeTest
    fun setup() {
        initDriver(DbType.RegularDelete)
    }
}