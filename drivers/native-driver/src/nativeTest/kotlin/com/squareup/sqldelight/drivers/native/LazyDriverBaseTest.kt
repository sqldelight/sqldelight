package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext.deleteDatabase
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.createDatabaseManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class LazyDriverBaseTest {
  protected lateinit var driver: NativeSqliteDriver
  private var manager: DatabaseManager? = null

  protected abstract val memory: Boolean

  private val transacterInternal: TransacterImpl by lazy {
    object : TransacterImpl(driver) {}
  }

  protected val transacter: TransacterImpl
    get() = transacterInternal

  @BeforeTest fun setup() {
    driver = setupDatabase(schema = defaultSchema())
  }

  @AfterTest fun tearDown() {
    driver.close()
  }

  protected fun defaultSchema(): SqlSchema<QueryResult.Value<Unit>> {
    return object : SqlSchema<QueryResult.Value<Unit>> {
      override val version: Long = 1

      override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        driver.execute(
          20,
          """
                  |CREATE TABLE test (
                  |  id INTEGER PRIMARY KEY,
                  |  value TEXT
                  |);
          """.trimMargin(),
          0,
        )
        driver.execute(
          30,
          """
                  |CREATE TABLE nullability_test (
                  |  id INTEGER PRIMARY KEY,
                  |  integer_value INTEGER,
                  |  text_value TEXT,
                  |  blob_value BLOB,
                  |  real_value REAL
                  |);
          """.trimMargin(),
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
    }
  }

  protected fun altInit(config: DatabaseConfiguration) {
    driver.close()
    driver = setupDatabase(defaultSchema(), config)
  }

  private fun setupDatabase(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    config: DatabaseConfiguration = defaultConfiguration(schema),
  ): NativeSqliteDriver {
    deleteDatabase(config.name!!)
    // This isn't pretty, but just for test
    manager = createDatabaseManager(config)
    return NativeSqliteDriver(manager!!)
  }

  protected fun defaultConfiguration(schema: SqlSchema<QueryResult.Value<Unit>>): DatabaseConfiguration {
    return DatabaseConfiguration(
      name = "testdb",
      version = 1,
      create = { connection ->
        wrapConnection(connection) {
          schema.create(it)
        }
      },
      extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 20_000),
      inMemory = true,
    )
  }
}
