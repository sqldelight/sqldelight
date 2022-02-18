package com.squareup.sqldelight.drivers.native

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext.deleteDatabase
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.stately.freeze
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
    get() {
      val t = transacterInternal
      t.freeze()
      return t
    }

  @BeforeTest fun setup() {
    driver = setupDatabase(schema = defaultSchema())
  }

  @AfterTest fun tearDown() {
    driver.close()
  }

  protected fun defaultSchema(): SqlDriver.Schema<SqlPreparedStatement, SqlCursor> {
    return object : SqlDriver.Schema<SqlPreparedStatement, SqlCursor> {
      override val version: Int = 1

      override fun create(driver: SqlDriver<SqlPreparedStatement, SqlCursor>) {
        driver.execute(
          20,
          """
                  |CREATE TABLE test (
                  |  id INTEGER PRIMARY KEY,
                  |  value TEXT
                  |);
                """.trimMargin(),
          0
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
          0
        )
      }

      override fun migrate(
        driver: SqlDriver<SqlPreparedStatement, SqlCursor>,
        oldVersion: Int,
        newVersion: Int
      ) {
        // No-op.
      }
    }
  }

  protected fun altInit(config: DatabaseConfiguration) {
    driver.close()
    driver = setupDatabase(defaultSchema(), config)
  }

  private fun setupDatabase(
    schema: SqlDriver.Schema<SqlPreparedStatement, SqlCursor>,
    config: DatabaseConfiguration = defaultConfiguration(schema)
  ): NativeSqliteDriver {
    deleteDatabase(config.name!!)
    // This isn't pretty, but just for test
    manager = createDatabaseManager(config)
    return NativeSqliteDriver(manager!!)
  }

  protected fun defaultConfiguration(
    schema: SqlDriver.Schema<SqlPreparedStatement, SqlCursor>,
  ): DatabaseConfiguration {
    return DatabaseConfiguration(
      name = "testdb",
      version = 1,
      create = { connection ->
        wrapConnection(connection) {
          schema.create(it)
        }
      },
      extendedConfig = DatabaseConfiguration.Extended(busyTimeout = 20_000),
      inMemory = true
    )
  }
}
