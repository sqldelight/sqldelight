package com.squareup.sqldelight.drivers.native.connectionpool

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.JournalMode
import co.touchlab.testhelp.concurrency.sleep
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.Worker
import kotlin.test.AfterTest
import kotlin.time.TimeSource

abstract class BaseConcurrencyTest {
  fun countRows(myDriver: SqlDriver = driver): Long {
    return myDriver.executeQuery(
      0,
      "SELECT count(*) FROM test",
      {
        it.next()
        QueryResult.Value(it.getLong(0)!!)
      },
      0,
    ).value
  }

  private var backingDriver: SqlDriver? = null
  private var dbName: String? = null
  internal val driver: SqlDriver
    get() = backingDriver!!

  internal inner class ConcurrentContext {
    private val myWorkers = arrayListOf<Worker>()
    internal fun createWorker(): Worker {
      val w = Worker.start()
      myWorkers.add(w)
      return w
    }

    internal fun stopWorkers() {
      myWorkers.forEach { it.requestTermination() }
    }
  }

  internal fun runConcurrent(block: ConcurrentContext.() -> Unit) {
    val context = ConcurrentContext()
    try {
      context.block()
    } finally {
      context.stopWorkers()
    }
  }

  fun setupDatabase(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    dbType: DbType,
    configBase: DatabaseConfiguration,
    maxReaderConnections: Int = 4,
  ): SqlDriver {
    // Some failing tests can leave the db in a weird state, so on each run we have a different db per test
    val name = "testdb_${globalDbCount.addAndGet(1)}"
    dbName = name
    DatabaseFileContext.deleteDatabase(name)
    val configCommon = configBase.copy(
      name = name,
      version = 1,
      create = { conn ->
        wrapConnection(conn) { driver ->
          schema.create(driver)
        }
      },
    )
    return when (dbType) {
      DbType.RegularWal -> {
        NativeSqliteDriver(
          configCommon,
          maxReaderConnections = maxReaderConnections,
        )
      }
      DbType.RegularDelete -> {
        val config = configCommon.copy(journalMode = JournalMode.DELETE)
        NativeSqliteDriver(
          config,
          maxReaderConnections = maxReaderConnections,
        )
      }
      DbType.InMemoryShared -> {
        val config = configCommon.copy(inMemory = true)
        NativeSqliteDriver(
          config,
          maxReaderConnections = maxReaderConnections,
        )
      }
      DbType.InMemorySingle -> {
        val config = configCommon.copy(name = null, inMemory = true)
        NativeSqliteDriver(
          config,
          maxReaderConnections = maxReaderConnections,
        )
      }
    }
  }

  enum class DbType {
    RegularWal,
    RegularDelete,
    InMemoryShared,
    InMemorySingle,
  }

  fun createDriver(
    dbType: DbType,
    configBase: DatabaseConfiguration = DatabaseConfiguration(name = null, version = 1, create = {}),
  ): SqlDriver {
    return setupDatabase(
      schema = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
          driver.execute(
            null,
            """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
            0,
          )
          return QueryResult.Unit
        }

        override fun migrate(
          driver: SqlDriver,
          oldVersion: Long,
          newVersion: Long,
          vararg callbacks: AfterVersion,
        ) = QueryResult.Unit
      },
      dbType,
      configBase,
    )
  }

  internal fun waitFor(timeout: Long = 10_000, block: () -> Boolean) {
    val start = TimeSource.Monotonic.markNow()
    var wasTimeout = false

    while (!block() && !wasTimeout) {
      sleep(200)
      wasTimeout = (TimeSource.Monotonic.markNow() - start).inWholeMilliseconds > timeout
    }

    if (wasTimeout) {
      throw IllegalStateException("Timeout $timeout exceeded")
    }
  }

  fun initDriver(dbType: DbType) {
    backingDriver = createDriver(dbType)
  }

  @AfterTest
  fun tearDown() {
    backingDriver?.close()
    dbName?.let { DatabaseFileContext.deleteDatabase(it) }
  }

  internal fun insertTestData(testData: TestData, driver: SqlDriver = this.driver) {
    driver.execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
      bindLong(0, testData.id)
      bindString(1, testData.value)
    }
  }

  internal data class TestData(val id: Long, val value: String)
}

private val globalDbCount = AtomicInt(0)
