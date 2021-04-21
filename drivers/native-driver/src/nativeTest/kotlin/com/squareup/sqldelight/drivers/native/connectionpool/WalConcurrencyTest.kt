package com.squareup.sqldelight.drivers.native.connectionpool

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.TransacterImpl
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Multiple connections are currently supported by WAL only. There
 * were issues with `JournalMode.DELETE` that would need some designing around.
 */
class WalConcurrencyTest : BaseConcurrencyTest() {
  @BeforeTest
  fun setup() {
    initDriver(DbType.RegularWal)
  }

  @Test
  fun readTransactions() {
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
        //Write transaction has written a row, but we don't see it.
        assertEquals(countRows(), 0)
      }
      transacter.transaction {
        assertEquals(countRows(), 1)
      }
    }

    val futures = (0 until times).map {
      val worker = Worker.start()
      Pair(
        worker,
        worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }
      )
    }

    //Wait for all read transactions to start
    waitFor { readStarted.value == times }

    transacter.transaction {
      insertTestData(TestData(1L, "arst 1"))
    }

    //Signal that write transaction is done
    writeFinished.value = 1

    futures.forEach {
      it.second.result
      it.first.requestTermination()
    }

    assertEquals(countRows(), 1L)
  }

  @Test
  fun writeNotBlockRead() {
    assertEquals(countRows(), 0)

    val transacter: TransacterImpl = object : TransacterImpl(driver) {}
    val worker = Worker.start()
    val counter = AtomicInt(0)
    val transactionStarted = AtomicInt(0)

    val block = {
      transacter.transaction {
        insertTestData(TestData(1L, "arst 1"))
        transactionStarted.increment()
        sleep(1500)
        counter.increment()
      }
    }

    val future = worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }

    //When ready, transaction started but sleeping
    waitFor { transactionStarted.value > 0 }

    //These three should run before transaction is done (not blocking)
    assertEquals(counter.value, 0)
    assertEquals(0L, countRows())
    assertEquals(counter.value, 0)

    future.result

    worker.requestTermination()
  }

  @Test
  fun writeBlocksWrite() {
    val transacter: TransacterImpl = object : TransacterImpl(driver) {}
    val worker = Worker.start()
    val counter = AtomicInt(0)
    val transactionStarted = AtomicInt(0)

    val block = {
      transacter.transaction {
        insertTestData(TestData(1L, "arst 1"))
        transactionStarted.increment()
        sleep(1500)
        counter.increment()
      }
    }

    val future = worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }

    //Transaction with write started but sleeping
    waitFor { transactionStarted.value > 0 }

    assertEquals(counter.value, 0)
    insertTestData(TestData(2L, "arst 2")) //This waits on transaction to wrap up
    assertEquals(counter.value, 1) //Counter would be zero if write didn't block (see above)

    future.result

    worker.requestTermination()
  }

  @Test
  fun multipleWritesDontTimeOut() {
    val transacter: TransacterImpl = object : TransacterImpl(driver) {}
    val worker = Worker.start()
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
      insertTestData(TestData(2L, "arst 2"), driver)
    }

    future.result
    worker.requestTermination()
  }

  /**
   * Just a bunch of inserts on multiple threads. More of a stress test.
   *
   * *NOTE* This can fail on Delete/Journal log db connection type.
   */
  @Test
  fun multiWrite() {
    val ops = ThreadOperations {}
    val times = 50_000
    val transacter: TransacterImpl = object : TransacterImpl(driver) {}

    repeat(times) { index ->
      ops.exe {
        transacter.transaction {
          insertTestData(TestData(index.toLong(), "arst $index"))

          val id2 = index.toLong() + times
          insertTestData(TestData(id2, "arst $id2"))

          val id3 = index.toLong() + times + times
          insertTestData(TestData(id3, "arst $id3"))
        }
      }
    }

    ops.run(10)

    assertEquals(countRows(), times.toLong() * 3)
  }
}