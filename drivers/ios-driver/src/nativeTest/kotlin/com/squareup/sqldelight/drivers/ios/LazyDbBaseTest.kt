package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.NativeFileContext.deleteDatabase
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class LazyDbBaseTest {
  protected lateinit var database: NativeSqlDatabase
  private var manager: DatabaseManager? = null

  protected abstract val memory: Boolean

  private val transacterInternal: Transacter by lazy {
    object : Transacter(database) {}
  }

  protected val transacter: Transacter
    get() {
      val t = transacterInternal
      t.freeze()
      return t
    }

  @BeforeTest fun setup() {
    database = setupDatabase(schema = defaultSchema())
  }

  @AfterTest fun tearDown() {
    database.close()
  }

  protected fun defaultSchema(): SqlDatabase.Schema {
    return object : SqlDatabase.Schema {
      override val version: Int = 1

      override fun create(db: SqlDatabase) {
        db.execute(20, """
                  |CREATE TABLE test (
                  |  id INTEGER PRIMARY KEY,
                  |  value TEXT
                  |);
                """.trimMargin(), 0)
        db.execute(30, """
                  |CREATE TABLE nullability_test (
                  |  id INTEGER PRIMARY KEY,
                  |  integer_value INTEGER,
                  |  text_value TEXT,
                  |  blob_value BLOB,
                  |  real_value REAL
                  |);
                """.trimMargin(), 0)
      }

      override fun migrate(
        db: SqlDatabase,
        oldVersion: Int,
        newVersion: Int
      ) {
        // No-op.
      }
    }
  }

  protected fun altInit(config: DatabaseConfiguration) {
    database.close()
    database = setupDatabase(defaultSchema(), config)
  }

  private fun setupDatabase(
    schema: SqlDatabase.Schema,
    config: DatabaseConfiguration = defaultConfiguration(schema)
  ): NativeSqlDatabase {
    deleteDatabase(config.name)
    //This isn't pretty, but just for test
    manager = createDatabaseManager(config)
    return NativeSqlDatabase(manager!!)
  }

  protected fun defaultConfiguration(schema: SqlDatabase.Schema): DatabaseConfiguration {
    return DatabaseConfiguration(
        name = "testdb",
        version = 1,
        inMemory = memory,
        create = { connection ->
          wrapConnection(connection) {
            schema.create(it)
          }
        },
        busyTimeout = 20_000)
  }

}