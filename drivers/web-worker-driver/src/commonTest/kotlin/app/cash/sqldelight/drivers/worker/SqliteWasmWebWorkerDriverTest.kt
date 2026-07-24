/*
 * Copyright (C) 2026 Block, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitQuery
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.SqliteWasmWebWorkerDriver
import app.cash.sqldelight.driver.worker.SqliteWasmWorkerConfig
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import app.cash.sqldelight.driver.worker.WebWorkerException
import app.cash.sqldelight.driver.worker.createSqliteWasmWebWorkerDriver
import app.cash.sqldelight.driver.worker.createSqliteWasmWorker
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SqliteWasmWebWorkerDriverTest {
  private var databaseId = 0

  @Test
  fun native_worker_requires_configuration_before_queries() = runTest {
    val driver = WebWorkerDriver(createSqliteWasmWorker())
    try {
      val exception = assertFailsWith<WebWorkerException> {
        driver.queryLong("SELECT 1")
      }

      assertContains(exception.message.orEmpty(), "Worker must be configured before use")
    } finally {
      driver.close()
    }
  }

  @Test
  fun parameters_crud_transactions_persist_across_worker_restart_and_cleanup() = runOpfsTest("crud") { database ->
    val firstDriver = database.open(crudSchema)
    assertEquals(
      0L,
      firstDriver.await(
        null,
        "CREATE TABLE counts (id INTEGER PRIMARY KEY, active INTEGER NOT NULL)",
        0,
      ),
    )
    assertEquals(
      2L,
      firstDriver.await(
        null,
        "INSERT INTO counts VALUES (1, 0), (2, 0)",
        0,
      ),
    )
    assertEquals(
      2L,
      firstDriver.await(
        null,
        "UPDATE counts SET active = 1",
        0,
      ),
    )
    assertEquals(
      1L,
      firstDriver.await(
        null,
        "DELETE FROM counts WHERE id = 2",
        0,
      ),
    )
    firstDriver.awaitQuery(
      identifier = null,
      sql = "SELECT active, 0 FROM counts",
      mapper = { cursor ->
        assertTrue(cursor.next().await())
        assertEquals(true, cursor.getBoolean(0))
        assertEquals(false, cursor.getBoolean(1))
      },
      parameters = 0,
    )

    firstDriver.await(
      identifier = null,
      sql = "INSERT INTO records VALUES (?, ?, ?, ?, ?)",
      parameters = 5,
    ) {
      bindLong(0, 1)
      bindString(1, "first")
      bindDouble(2, 1.5)
      bindBytes(3, byteArrayOf(1, 2, 3))
      bindString(4, null)
    }

    firstDriver.awaitQuery(
      identifier = null,
      sql = "SELECT id, name, score, payload, note FROM records WHERE id = ?",
      mapper = { cursor ->
        assertTrue(cursor.next().await())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("first", cursor.getString(1))
        assertEquals(1.5, cursor.getDouble(2))
        assertContentEquals(byteArrayOf(1, 2, 3), cursor.getBytes(3))
        assertNull(cursor.getString(4))
        assertFalse(cursor.next().await())
      },
      parameters = 1,
      binders = { bindLong(0, 1) },
    )

    firstDriver.await(
      identifier = null,
      sql = "UPDATE records SET name = ? WHERE id = ?",
      parameters = 2,
    ) {
      bindString(0, "updated")
      bindLong(1, 1)
    }
    assertEquals("updated", firstDriver.queryString("SELECT name FROM records WHERE id = 1"))

    val transacter = object : SuspendingTransacterImpl(firstDriver) {}
    transacter.transaction {
      firstDriver.await(
        identifier = null,
        sql = "INSERT INTO records (id, name) VALUES (?, ?)",
        parameters = 2,
      ) {
        bindLong(0, 2)
        bindString(1, "committed")
      }
    }
    transacter.transaction {
      firstDriver.await(
        identifier = null,
        sql = "INSERT INTO records (id, name) VALUES (?, ?)",
        parameters = 2,
      ) {
        bindLong(0, 3)
        bindString(1, "rolled back")
      }
      rollback()
    }

    firstDriver.await(null, "DELETE FROM records WHERE id = ?", 1) {
      bindLong(0, 1)
    }
    assertEquals(1L, firstDriver.queryLong("SELECT COUNT(*) FROM records"))
    database.close(firstDriver)

    // createSqliteWasmWebWorkerDriver creates a new Worker, so this assertion cannot be
    // satisfied by state held only in the first worker's memory.
    val restartedDriver = database.open()
    assertEquals("committed", restartedDriver.queryString("SELECT name FROM records WHERE id = 2"))
    assertEquals(0L, restartedDriver.queryLong("SELECT COUNT(*) FROM records WHERE id = 3"))
    database.delete(restartedDriver)

    val afterDeleteDriver = database.open()
    assertEquals(
      0L,
      afterDeleteDriver.queryLong(
        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'records'",
      ),
    )
    database.delete(afterDeleteDriver)
  }

  @Test
  fun schema_bootstrap_and_migration_preserve_data_and_advance_user_version() = runOpfsTest("migration") { database ->
    val versionOneDriver = database.open(versionOneSchema)
    assertEquals(1L, versionOneDriver.queryLong("PRAGMA user_version"))
    database.close(versionOneDriver)

    var migrationRan = false
    var afterVersionRan = false
    val versionTwoSchema = versionTwoSchema {
      migrationRan = true
    }
    val versionTwoDriver = database.open(
      versionTwoSchema,
      AfterVersion(1) {
        afterVersionRan = true
      },
    )

    assertTrue(migrationRan)
    assertTrue(afterVersionRan)
    assertEquals("existing", versionTwoDriver.queryString("SELECT value FROM items WHERE id = 1"))
    assertEquals("added by migration", versionTwoDriver.queryString("SELECT detail FROM items WHERE id = 1"))
    assertEquals("v2", versionTwoDriver.queryString("SELECT value FROM schema_state"))
    assertEquals(2L, versionTwoDriver.queryLong("PRAGMA user_version"))
    database.close(versionTwoDriver)

    val restartedDriver = database.open()
    assertEquals("existing", restartedDriver.queryString("SELECT value FROM items WHERE id = 1"))
    assertEquals("v2", restartedDriver.queryString("SELECT value FROM schema_state"))
    assertEquals(2L, restartedDriver.queryLong("PRAGMA user_version"))
    database.delete(restartedDriver)
  }

  @Test
  fun failed_migration_rolls_back_schema_and_user_version() = runOpfsTest("failed-migration") { database ->
    val versionOneDriver = database.open(versionOneSchema)
    database.close(versionOneDriver)

    assertFailsWith<FailedMigration> {
      database.open(failingVersionTwoSchema)
    }

    val restartedDriver = database.open()
    assertEquals(1L, restartedDriver.queryLong("PRAGMA user_version"))
    assertEquals(
      0L,
      restartedDriver.queryLong(
        "SELECT COUNT(*) FROM pragma_table_info('items') WHERE name = 'failed_column'",
      ),
    )
    assertEquals("existing", restartedDriver.queryString("SELECT value FROM items WHERE id = 1"))
    database.delete(restartedDriver)
  }

  private fun runOpfsTest(
    name: String,
    block: suspend (TestDatabase) -> Unit,
  ) = runTest {
    val database = TestDatabase(
      SqliteWasmWorkerConfig(
        databaseName = "sqldelight-opfs-$name-${databaseId++}-${Random.nextLong()}.db",
      ),
    )
    try {
      block(database)
    } finally {
      database.cleanup()
    }
  }

  private class TestDatabase(
    private val config: SqliteWasmWorkerConfig,
  ) {
    private val openDrivers = mutableListOf<SqliteWasmWebWorkerDriver>()
    private var deleted = false

    suspend fun open(): SqliteWasmWebWorkerDriver {
      return createSqliteWasmWebWorkerDriver(config).also {
        deleted = false
        openDrivers.add(it)
      }
    }

    suspend fun open(
      schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
      vararg callbacks: AfterVersion,
    ): SqliteWasmWebWorkerDriver {
      return createSqliteWasmWebWorkerDriver(
        schema = schema,
        config = config,
        callbacks = callbacks,
      ).also {
        deleted = false
        openDrivers.add(it)
      }
    }

    suspend fun close(driver: SqliteWasmWebWorkerDriver) {
      try {
        driver.closeAndAwait()
      } finally {
        openDrivers.remove(driver)
      }
    }

    suspend fun delete(driver: SqliteWasmWebWorkerDriver) {
      try {
        driver.deleteDatabase()
        deleted = true
      } finally {
        openDrivers.remove(driver)
      }
    }

    suspend fun cleanup() {
      openDrivers.toList().forEach { driver ->
        try {
          driver.closeAndAwait()
        } catch (_: Throwable) {
          driver.close()
        }
      }
      openDrivers.clear()
      if (!deleted) {
        createSqliteWasmWebWorkerDriver(config).deleteDatabase()
        deleted = true
      }
    }
  }

  private class FailedMigration : RuntimeException("migration failed")

  private companion object {
    val crudSchema = object : SqlSchema<QueryResult.AsyncValue<Unit>> {
      override val version = 1L

      override fun create(driver: SqlDriver) = QueryResult.AsyncValue {
        driver.await(
          identifier = null,
          sql = """
            CREATE TABLE records (
              id INTEGER PRIMARY KEY,
              name TEXT NOT NULL,
              score REAL,
              payload BLOB,
              note TEXT
            )
          """.trimIndent(),
          parameters = 0,
        )
        Unit
      }

      override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
      ) = QueryResult.AsyncValue<Unit> {}
    }

    val versionOneSchema = object : SqlSchema<QueryResult.AsyncValue<Unit>> {
      override val version = 1L

      override fun create(driver: SqlDriver) = QueryResult.AsyncValue {
        driver.await(
          null,
          "CREATE TABLE items (id INTEGER PRIMARY KEY, value TEXT NOT NULL)",
          0,
        )
        driver.await(
          null,
          "INSERT INTO items (id, value) VALUES (?, ?)",
          2,
        ) {
          bindLong(0, 1)
          bindString(1, "existing")
        }
        Unit
      }

      override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
      ) = QueryResult.AsyncValue<Unit> {}
    }

    fun versionTwoSchema(onMigration: () -> Unit) = object : SqlSchema<QueryResult.AsyncValue<Unit>> {
      override val version = 2L

      override fun create(driver: SqlDriver) = QueryResult.AsyncValue {
        driver.await(
          null,
          """
              CREATE TABLE items (
                id INTEGER PRIMARY KEY,
                value TEXT NOT NULL,
                detail TEXT
              )
          """.trimIndent(),
          0,
        )
        driver.await(null, "CREATE TABLE schema_state (value TEXT NOT NULL)", 0)
        Unit
      }

      override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
      ) = QueryResult.AsyncValue {
        assertEquals(1L, oldVersion)
        assertEquals(2L, newVersion)
        onMigration()

        driver.await(null, "ALTER TABLE items ADD COLUMN detail TEXT", 0)
        driver.await(
          null,
          "UPDATE items SET detail = 'added by migration' WHERE id = 1",
          0,
        )
        driver.await(null, "CREATE TABLE schema_state (value TEXT NOT NULL)", 0)
        driver.await(null, "INSERT INTO schema_state VALUES ('v2')", 0)
        callbacks
          .filter { it.afterVersion in oldVersion until newVersion }
          .sortedBy(AfterVersion::afterVersion)
          .forEach { it.block(driver) }
        Unit
      }
    }

    val failingVersionTwoSchema = object : SqlSchema<QueryResult.AsyncValue<Unit>> {
      override val version = 2L

      override fun create(driver: SqlDriver) = QueryResult.AsyncValue<Unit> {}

      override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
      ) = QueryResult.AsyncValue<Unit> {
        driver.await(null, "ALTER TABLE items ADD COLUMN failed_column TEXT", 0)
        driver.await(null, "PRAGMA user_version = 99", 0)
        throw FailedMigration()
      }
    }
  }
}

private suspend fun SqlDriver.queryLong(
  sql: String,
  parameters: Int = 0,
  binders: (SqlPreparedStatement.() -> Unit)? = null,
): Long? {
  return awaitQuery(null, sql, ::firstLong, parameters, binders)
}

private suspend fun SqlDriver.queryString(
  sql: String,
  parameters: Int = 0,
  binders: (SqlPreparedStatement.() -> Unit)? = null,
): String? {
  return awaitQuery(null, sql, ::firstString, parameters, binders)
}

private suspend fun firstLong(cursor: SqlCursor): Long? {
  return if (cursor.next().await()) cursor.getLong(0) else null
}

private suspend fun firstString(cursor: SqlCursor): String? {
  return if (cursor.next().await()) cursor.getString(0) else null
}
