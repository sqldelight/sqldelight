package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement
import app.cash.sqldelight.driver.sqljs.initAsyncSqlDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

typealias InsertFunction = suspend (AsyncSqlPreparedStatement.() -> Unit) -> Unit

class JsWorkerDriverTest {
  private val schema = object : AsyncSqlDriver.Schema {
    override val version: Int = 1

    override suspend fun create(driver: AsyncSqlDriver) {
      driver.execute(
        0,
        """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(),
        0
      )
      driver.execute(
        1,
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

    override suspend fun migrate(driver: AsyncSqlDriver, oldVersion: Int, newVersion: Int) {
      // No-op.
    }
  }

  private fun runTest(block: suspend (AsyncSqlDriver) -> Unit) = kotlinx.coroutines.test.runTest {
    val driver = setup()
    // Tests are skipped if running on Node
    if (driver != null) {
      block(driver)
      tearDown(driver)
    }
  }

  private suspend fun setup(): AsyncSqlDriver? {
    if (IS_NODE) return null
    return initAsyncSqlDriver("/worker.sql-wasm.js", schema)
  }

  private suspend fun tearDown(driver: AsyncSqlDriver) {
    driver.close()
  }

  @Test
  fun insert_can_run_multiple_times() = runTest { driver ->

    val insert: InsertFunction = { binders: AsyncSqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    suspend fun query(mapper: (AsyncSqlCursor) -> Unit) {
      driver.executeQuery(3, "SELECT * FROM test", mapper, 0)
    }

    suspend fun changes(mapper: (AsyncSqlCursor) -> Long?): Long? {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0)
    }

    query {
      assertFalse(it.next())
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }

    query {
      assertTrue(it.next())
      assertFalse(it.next())
    }

    assertEquals(1, changes { it.next(); it.getLong(0) })

    query {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
    }

    insert {
      bindLong(1, 2)
      bindString(2, "Jake")
    }
    assertEquals(1, changes { it.next(); it.getLong(0) })

    query {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    driver.execute(5, "DELETE FROM test", 0)
    assertEquals(2, changes { it.next(); it.getLong(0) })

    query {
      assertFalse(it.next())
    }
  }

  @Test
  fun query_can_run_multiple_times() = runTest { driver ->

    val insert: InsertFunction = { binders: AsyncSqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    suspend fun changes(mapper: (AsyncSqlCursor) -> Long?): Long? {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0)
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }
    assertEquals(1, changes { it.next(); it.getLong(0) })
    insert {
      bindLong(1, 2)
      bindString(2, "Jake")
    }
    assertEquals(1, changes { it.next(); it.getLong(0) })

    suspend fun query(binders: AsyncSqlPreparedStatement.() -> Unit, mapper: (AsyncSqlCursor) -> Unit) {
      driver.executeQuery(6, "SELECT * FROM test WHERE value = ?", mapper, 1, binders)
    }
    query(
            binders = {
              bindString(1, "Jake")
            },
            mapper = {
              assertTrue(it.next())
              assertEquals(2, it.getLong(0))
              assertEquals("Jake", it.getString(1))
            }
    )

    // Second time running the query is fine
    query(
            binders = {
              bindString(1, "Jake")
            },
            mapper = {
              assertTrue(it.next())
              assertEquals(2, it.getLong(0))
              assertEquals("Jake", it.getString(1))
            }
    )
  }

  @Test
  fun sqlResultSet_getters_return_null_if_the_column_values_are_NULL() = runTest { driver ->
    val insert: InsertFunction = { binders: AsyncSqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }

    suspend fun changes(mapper: (AsyncSqlCursor) -> Long?): Long? {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0)
    }

    val inserted = insert {
      bindLong(1, 1)
      bindLong(2, null)
      bindString(3, null)
      bindBytes(4, null)
      bindDouble(5, null)
    }

    val mapper: (AsyncSqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0)
    changes { it.next(); it.getLong(0) }
  }

  @Test
  fun types_are_correctly_converted_from_JS_to_Kotlin_and_back() = runTest { driver ->
    val insert: InsertFunction = { binders: AsyncSqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }

    insert {
      bindLong(1, 1)
      bindLong(2, Long.MAX_VALUE)
      bindString(3, "Hello")
      bindBytes(4, ByteArray(5) { it.toByte() })
      bindDouble(5, Float.MAX_VALUE.toDouble())
    }

    val mapper: (AsyncSqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals(Long.MAX_VALUE, it.getLong(1))
      assertEquals("Hello", it.getString(2))
      it.getBytes(3)?.forEachIndexed { index, byte -> assertEquals(index.toByte(), byte) }
      assertEquals(Float.MAX_VALUE.toDouble(), it.getDouble(4))
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0)
  }

  companion object {
    private val IS_NODE: Boolean = js(
      "typeof process !== 'undefined' && process.versions != null && process.versions.node != null"
    ) as Boolean
  }
}
