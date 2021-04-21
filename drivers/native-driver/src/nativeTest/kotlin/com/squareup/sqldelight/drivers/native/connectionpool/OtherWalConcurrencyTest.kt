package com.squareup.sqldelight.drivers.native.connectionpool

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.TransacterImpl
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.freeze
import kotlin.test.Test
import kotlin.test.assertFails

/**
 * Tests with explicit driver creation to test different connection counts. May fold into `WalConcurrencyTest`
 * at some point. Currently just demonstrating the potential issue with multiple transaction connections.
 */
class OtherWalConcurrencyTest : BaseConcurrencyTest() {

  @Test
  fun transactionConflictsSingleOk(){
    transactionConflicts(1)
  }

  @Test
  fun transactionConflictsMultipleBad(){
    assertFails { transactionConflicts(4) }
  }

  /**
   * This is a potential issue introduced with multiple transaction connections being able to write. If there are 2 overlapping connections,
   * both can read and not block. Both can write, and the second one to start writing will wait for the first. One can write
   * while the other is reading.
   *
   * However, if transaction 1 is writing, and transaction 2 reads and then attempts to write, it'll fail with a
   * SQLITE_BUSY. As a result, while the driver allows you to try using multiple connections, it'll fail at runtime
   * if your transactions happen to run in the wrong sequence.
   */
  internal fun transactionConflicts(connections:Int) = runConcurrent{
    val driver = createDriver(
      DbType.RegularWal,
      DatabaseConfiguration(
        name = null,
        version = 1,
        create = {}
      ),
    connections
    )

    val transacter: TransacterImpl = object : TransacterImpl(driver) {}
    val worker = createWorker()
    val transactionStarted = AtomicInt(0)

    val block = {
      transacter.transaction {
        insertTestData(TestData(1L, "arst 1"), driver)
        transactionStarted.increment()
        sleep(1500)
        insertTestData(TestData(5L, "arst 1"), driver)
      }
    }

    val future = worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }

    //When we get here, first transaction has run a write command, and is sleeping
    waitFor { transactionStarted.value > 0 }

    transacter.transaction {
      countRows(driver) //Force read
      insertTestData(TestData(9L, "arst 2"), driver)
    }

    future.result
  }
}