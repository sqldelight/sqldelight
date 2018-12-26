package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.test.BeforeTest
import co.touchlab.sqliter.NativeFileContext.deleteDatabase
import co.touchlab.sqliter.*
import co.touchlab.stately.freeze
import com.squareup.sqldelight.Transacter
import kotlin.test.AfterTest

abstract class LazyDbBaseTest {
  private var internalDb: SqlDatabase? = null

  protected var manager: DatabaseManager? = null
  protected abstract val memory: Boolean

  /**
   * Lazy, in case you don't really need it. If we push a specific setup from the test method,
   * internalDb won't be null when we eval the lazy init.
   */
  protected val database: SqlDatabase by lazy {
    val db = internalDb ?: setupDatabase(
        schema = defaultSchema()
    )
    internalDb = db
    db
  }

  private val transacterInternal: Transacter by lazy {
    object : Transacter(database) {}
  }

  protected val transacter: Transacter
    get() {
      val t = transacterInternal
      t.freeze()
      return t
    }

  @AfterTest fun tearDown() {
    internalDb?.let { it.close() }
  }

  protected fun defaultSchema(): SqlDatabase.Schema {
    return object : SqlDatabase.Schema {
      override val version: Int = 1

      override fun create(db: SqlDatabaseConnection) {
        db.prepareStatement(
            """
                  |CREATE TABLE test (
                  |  id INTEGER PRIMARY KEY,
                  |  value TEXT
                  |);
                """.trimMargin(), SqlPreparedStatement.Type.EXECUTE, 0
        )
            .execute()
        db.prepareStatement(
            """
                  |CREATE TABLE nullability_test (
                  |  id INTEGER PRIMARY KEY,
                  |  integer_value INTEGER,
                  |  text_value TEXT,
                  |  blob_value BLOB,
                  |  real_value REAL
                  |);
                """.trimMargin(), SqlPreparedStatement.Type.EXECUTE, 0
        )
            .execute()
      }

      override fun migrate(
        db: SqlDatabaseConnection,
        oldVersion: Int,
        newVersion: Int
      ) {
        // No-op.
      }
    }
  }

  protected fun altInit(config: DatabaseConfiguration) {
    internalDb = setupDatabase(defaultSchema(), config)
  }

  fun setupDatabase(
    schema: SqlDatabase.Schema,
    config: DatabaseConfiguration = defaultConfiguration(schema)
  ): SqlDatabase {

    deleteDatabase(config.name)
    //This isn't pretty, but just for test
    manager = createDatabaseManager(config)
    return NativeSqlDatabase(manager!!)
  }

  protected fun defaultConfiguration(schema: SqlDatabase.Schema): DatabaseConfiguration {
    return DatabaseConfiguration(
        name = "testdb",
        version = 1,
        inMemory = memory(),
        create = { connection ->
          wrapConnection(connection) {
            schema.create(it)
          }
        },
        busyTimeout = 20_000)
  }

}