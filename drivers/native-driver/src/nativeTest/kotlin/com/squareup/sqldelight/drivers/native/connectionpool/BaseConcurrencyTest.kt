package com.squareup.sqldelight.drivers.native.connectionpool

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.JournalMode
import co.touchlab.stately.ensureNeverFrozen
import co.touchlab.testhelp.concurrency.currentTimeMillis
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.squareup.sqldelight.drivers.native.wrapConnection
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.native.concurrent.Worker
import kotlin.test.AfterTest

abstract class BaseConcurrencyTest {
  fun countRows(myDriver:SqlDriver = driver): Long {
    val cur = myDriver.executeQuery(0, "SELECT count(*) FROM test", 0)
    try {
      cur.next()
      val count = cur.getLong(0)
      return count!!
    } finally {
      cur.close()
    }
  }

  private var _driver: SqlDriver? = null
  internal val driver: SqlDriver
    get() = _driver!!

  internal inner class ConcurrentContext{
    private val myWorkers = arrayListOf<Worker>()
    internal fun createWorker():Worker{
      val w = Worker.start()
      myWorkers.add(w)
      return w
    }
    internal fun stopWorkers(){
      myWorkers.forEach { it.requestTermination() }
    }
  }


  internal fun runConcurrent(block:ConcurrentContext.()->Unit){
    val context = ConcurrentContext()
    try {
      context.block()
    } finally {
      context.stopWorkers()
    }
  }

  fun setupDatabase(schema: SqlDriver.Schema,
                    dbType: DbType,
                    configBase: DatabaseConfiguration,
                    maxTransactionConnections:Int = 4,
                    maxReaderConnections:Int = 4
  ): SqlDriver {
    val name = "testdb"
    DatabaseFileContext.deleteDatabase(name)
    val configCommon = configBase.copy(
      name = name,
      version = 1,
      create = { conn ->
        wrapConnection(conn) { driver ->
          schema.create(driver)
        }
      }
    )
    return when (dbType) {
      DbType.RegularWal -> {
        NativeSqliteDriver(configCommon,
          maxTransactionConnections = maxTransactionConnections,
          maxReaderConnections = maxReaderConnections)
      }
      DbType.RegularDelete -> {
        val config = configCommon.copy(journalMode = JournalMode.DELETE)
        NativeSqliteDriver(config,
          maxTransactionConnections = maxTransactionConnections,
          maxReaderConnections = maxReaderConnections)
      }
      DbType.InMemoryShared -> {
        val config = configCommon.copy(inMemory = true)
        NativeSqliteDriver(config,
          maxTransactionConnections = maxTransactionConnections,
          maxReaderConnections = maxReaderConnections)
      }
      DbType.InMemorySingle -> {
        val config = configCommon.copy(name = null, inMemory = true)
        NativeSqliteDriver(config,
          maxTransactionConnections = maxTransactionConnections,
          maxReaderConnections = maxReaderConnections)
      }
    }
  }

  enum class DbType {
    RegularWal, RegularDelete, InMemoryShared, InMemorySingle
  }

  fun createDriver(
    dbType: DbType,
    configBase: DatabaseConfiguration = DatabaseConfiguration(name = null, version = 1, create = {}),
    maxTransactionConnections:Int = 4,
  ): SqlDriver {
    return setupDatabase(
      schema = object : SqlDriver.Schema {
        override val version: Int = 1

        override fun create(driver: SqlDriver) {
          driver.execute(
            null,
            """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
            0
          )
        }

        override fun migrate(
          driver: SqlDriver,
          oldVersion: Int,
          newVersion: Int
        ) {
          // No-op.
        }
      },
      dbType,
      configBase,
      maxTransactionConnections = maxTransactionConnections
    )
  }

  internal fun waitFor(timeout: Long = 10_000, block: () -> Boolean) {
    val start = currentTimeMillis()
    var wasTimeout = false

    while (!block() && !wasTimeout) {
      sleep(200)
      wasTimeout = (currentTimeMillis() - start) > timeout
    }

    if (wasTimeout)
      throw IllegalStateException("Timeout $timeout exceeded")
  }

  fun initDriver(dbType: DbType) {
    _driver = createDriver(dbType, maxTransactionConnections = 4)
  }

  @AfterTest
  fun tearDown() {
    _driver?.close()
  }

  internal fun insertTestData(testData: TestData, driver: SqlDriver = this.driver) {
    driver.execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
      bindLong(1, testData.id)
      bindString(2, testData.value)
    }
  }

  internal data class TestData(val id: Long, val value: String)
}