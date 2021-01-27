package com.squareup.sqldelight.drivers.native

import com.squareup.sqldelight.TransacterImpl
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WalConcurrencyTest: ConcurrencyTest() {
    @BeforeTest
    fun setup() {
        initDriver(DbType.RegularWal)
    }

    @Test
    fun readTransactions(){
        val times = 3
        assertEquals(countRows(), 0)
        val transacter: TransacterImpl = object : TransacterImpl(driver) {}

        val readStarted = AtomicInt(0)
        val writeFinished = AtomicInt(0)

        val block = {
            transacter.transaction {
                assertEquals(countRows(), 0)
                readStarted.increment()
                waitFor { writeFinished.value != 0 }
                assertEquals(countRows(), 0)
            }
        }

        val futures = (0 until times).map {
            val worker = Worker.start()
            Pair(
                worker,
                worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }
            )
        }

        waitFor { readStarted.value == times }

        transacter.transaction {
            insertTestData(TestData(1L, "arst 1"))
        }

        writeFinished.value = 1

        futures.forEach {
            it.second.result
            it.first.requestTermination()
        }

        assertEquals(countRows(), 1L)
    }


}