package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.TransacterImpl
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.Test

class OtherConcurrencyTest : BaseConcurrencyTest() {

  @Test
  fun multipleWritesDontTimeOutWAL() {
    multipleWritesDontTimeOut(DbType.RegularWal)
  }

  @Test
  fun multipleWritesDontTimeOutDELETE() {
    multipleWritesDontTimeOut(DbType.RegularDelete)
  }

  fun multipleWritesDontTimeOut(dbType: DbType) {
    val driver = createDriver(
      dbType,
      DatabaseConfiguration(
        name = null,
        version = 1,
        create = {},
        extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 500)
      )
    )

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

    waitFor { transactionStarted.value > 0 }
    transacter.transaction {
      insertTestData(TestData(2L, "arst 2"), driver)
    }

    future.result
    worker.requestTermination()
  }
}