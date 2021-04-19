package com.squareup.sqldelight.drivers.native

import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.currentTimeMillis
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.TransacterImpl
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

abstract class ConcurrencyTest : BaseConcurrencyTest() {

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

    waitFor { transactionStarted.value > 0 }

    assertEquals(counter.value, 0)
    assertEquals(0L, countRows())
    assertEquals(counter.value, 0)

    future.result
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

    waitFor { transactionStarted.value > 0 }

    assertEquals(counter.value, 0)
    insertTestData(TestData(2L, "arst 2"))
    assertEquals(counter.value, 1)

    future.result
  }

  @Test
  fun multiWrite() {
    val ops = ThreadOperations {}
    val times = 1_000
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

  @Test
  fun multiRead() {
    assertEquals(countRows(), 0)

    val start = currentTimeMillis()
    val queryCount = AtomicInt(0)
    val ops = ThreadOperations {}
    val runs = 200
    repeat(runs) {
      ops.exe {
        assertEquals(countRows(), 0)
        queryCount.increment()
      }
    }

    ops.run(10)

    val transacter: TransacterImpl = object : TransacterImpl(driver) {}

    transacter.transaction {
      insertTestData(TestData(1234L, "arst"))
      var wasTimeout = false
      while (queryCount.value != runs && !wasTimeout) {
        println("queryCount.value ${queryCount.value}")
        sleep(500)
        wasTimeout = (currentTimeMillis() - start) > 5000
      }

      assertFalse(wasTimeout, "Test timed out")
    }

    assertEquals(countRows(), 1)
  }
}