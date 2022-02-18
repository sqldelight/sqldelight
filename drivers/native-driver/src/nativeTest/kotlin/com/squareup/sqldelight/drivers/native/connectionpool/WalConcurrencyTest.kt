package com.squareup.sqldelight.drivers.native.connectionpool

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.sleep
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testing multiple read and transaction pool connections. These were
 * written when it a was a single pool, so this will need some refactor.
 * The reader pool is much more likely to be multiple with a single transaction pool
 * connection, which removes a lot of the potential concurrency issues, but introduces new things
 * we should probably test.
 */
class WalConcurrencyTest : BaseConcurrencyTest() {
  @BeforeTest
  fun setup() {
    initDriver(DbType.RegularWal)
  }

  /**
   * This is less important now that we have a separate reader pool again, but will revisit.
   */
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

    // When ready, transaction started but sleeping
    waitFor { transactionStarted.value > 0 }

    // These three should run before transaction is done (not blocking)
    assertEquals(counter.value, 0)
    assertEquals(0L, countRows())
    assertEquals(counter.value, 0)

    future.result

    worker.requestTermination()
  }

  /**
   * Reader pool stress test
   */
  @Test @Ignore
  fun manyReads() = runConcurrent {
    val transacter: TransacterImpl = object : TransacterImpl(driver) {}
    val dataSize = 2_000
    transacter.transaction {
      repeat(dataSize) {
        insertTestData(TestData(it.toLong(), "Data $it"))
      }
    }

    val ops = ThreadOperations {}
    val totalCount = AtomicInt(0)
    val queryRuns = 100
    repeat(queryRuns) {
      ops.exe {
        totalCount.addAndGet(testDataQuery().executeAsList().size)
      }
    }
    ops.run(6)
    assertEquals(totalCount.value, dataSize * queryRuns)
    val readerPool = (driver as NativeSqliteDriver).readerPool
    // Make sure we actually created all of the connections
    assertTrue(readerPool.entryCount() > 1, "Reader pool size ${readerPool.entryCount()}")
  }

  private val mapper = { cursor: SqlCursor ->
    TestData(
      cursor.getLong(0)!!, cursor.getString(1)!!
    )
  }

  private fun testDataQuery(): Query<TestData, SqlCursor> {
    return object : Query<TestData, SqlCursor>(mapper) {
      override fun execute(): SqlCursor {
        return driver.executeQuery(0, "SELECT * FROM test", 0)
      }

      override fun addListener(listener: Listener) {
        driver.addListener(listener, arrayOf("test"))
      }

      override fun removeListener(listener: Listener) {
        driver.removeListener(listener, arrayOf("test"))
      }
    }
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

    // Transaction with write started but sleeping
    waitFor { transactionStarted.value > 0 }

    assertEquals(counter.value, 0)
    insertTestData(TestData(2L, "arst 2")) // This waits on transaction to wrap up
    assertEquals(counter.value, 1) // Counter would be zero if write didn't block (see above)

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

    // When we get here, first transaction has run a write command, and is sleeping
    waitFor { transactionStarted.value > 0 }
    transacter.transaction {
      insertTestData(TestData(2L, "arst 2"), driver)
    }

    future.result
    worker.requestTermination()
  }

  /**
   * Just a bunch of inserts on multiple threads. More of a stress test.
   */
  @Test
  fun multiWrite() {
    val ops = ThreadOperations {}
    val times = 10_000
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
