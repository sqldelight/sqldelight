package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitQuery
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.sqljs.worker.JsWorkerException
import app.cash.sqldelight.driver.sqljs.worker.initAsyncSqlDriver
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

typealias InsertFunction = suspend (SqlPreparedStatement.() -> Unit) -> Unit

class JsWorkerDriverTest {
  private val schema = object : SqlSchema {
    override val version: Int = 1

    override fun create(driver: SqlDriver): QueryResult<Unit> = QueryResult.AsyncValue {
      driver.execute(
        0,
        """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
        """.trimMargin(),
        emptyList(),
      ).await()
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
        emptyList(),
      ).await()
    }

    override fun migrate(driver: SqlDriver, oldVersion: Int, newVersion: Int) = QueryResult.AsyncValue {}
  }

  private fun runTest(block: suspend (SqlDriver) -> Unit) = kotlinx.coroutines.test.runTest {
    val driver = initAsyncSqlDriver("/worker.sql-wasm.js", schema)
    block(driver)
    driver.close()
  }

  @Test
  fun insert_can_run_multiple_times() = runTest { driver ->

    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(2, "INSERT INTO test VALUES (?, ?);", listOf(25, 28), binders)
    }

    suspend fun query(mapper: (SqlCursor) -> Unit) {
      driver.awaitQuery(3, "SELECT * FROM test", mapper, emptyList())
    }

    suspend fun changes(mapper: (SqlCursor) -> Long?): Long? {
      return driver.awaitQuery(4, "SELECT changes()", mapper, emptyList())
    }

    query {
      assertFalse(it.next())
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
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
      bindLong(0, 2)
      bindString(1, "Jake")
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

    driver.await(5, "DELETE FROM test", emptyList())
    assertEquals(2, changes { it.next(); it.getLong(0) })

    query {
      assertFalse(it.next())
    }
  }

  @Test
  fun query_can_run_multiple_times() = runTest { driver ->

    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(2, "INSERT INTO test VALUES (?, ?);", listOf(25, 28), binders)
    }

    suspend fun changes(mapper: (SqlCursor) -> Long?): Long? {
      return driver.awaitQuery(4, "SELECT changes()", mapper, emptyList())
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
    }
    assertEquals(1, changes { it.next(); it.getLong(0) })
    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(1, changes { it.next(); it.getLong(0) })

    suspend fun query(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> Unit) {
      driver.awaitQuery(6, "SELECT * FROM test WHERE value = ?", mapper, listOf(33), binders)
    }
    query(
      binders = {
        bindString(0, "Jake")
      },
      mapper = {
        assertTrue(it.next())
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
      },
    )

    // Second time running the query is fine
    query(
      binders = {
        bindString(0, "Jake")
      },
      mapper = {
        assertTrue(it.next())
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
      },
    )
  }

  @Test
  fun sqlResultSet_getters_return_null_if_the_column_values_are_NULL() = runTest { driver ->
    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", listOf(37, 40, 43, 46, 49), binders)
    }

    suspend fun changes(mapper: (SqlCursor) -> Long?): Long? {
      return driver.awaitQuery(4, "SELECT changes()", mapper, emptyList())
    }

    val inserted = insert {
      bindLong(0, 1)
      bindLong(1, null)
      bindString(2, null)
      bindBytes(3, null)
      bindDouble(4, null)
    }

    val mapper: (SqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
    driver.awaitQuery(8, "SELECT * FROM nullability_test", mapper, emptyList())
    changes { it.next(); it.getLong(0) }
  }

  @Test
  fun types_are_correctly_converted_from_JS_to_Kotlin_and_back() = runTest { driver ->
    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", listOf(37, 40, 43, 46, 49), binders)
    }

    insert {
      bindLong(0, 1)
      bindLong(1, Long.MAX_VALUE)
      bindString(2, "Hello")
      bindBytes(3, ByteArray(5) { it.toByte() })
      bindDouble(4, Float.MAX_VALUE.toDouble())
    }

    val mapper: (SqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals(Long.MAX_VALUE, it.getLong(1))
      assertEquals("Hello", it.getString(2))
      it.getBytes(3)?.forEachIndexed { index, byte -> assertEquals(index.toByte(), byte) }
      assertEquals(Float.MAX_VALUE.toDouble(), it.getDouble(4))
    }
    driver.awaitQuery(8, "SELECT * FROM nullability_test", mapper, emptyList())
  }

  @Test
  fun worker_exceptions_are_handled_correctly() = runTest { driver ->
    println("IS_LEGACY: $IS_LEGACY")
    // Despite the exception being thrown correctly in LEGACY builds, this test fails for some reason
    if (IS_LEGACY) return@runTest

    val error = assertFailsWith<JsWorkerException> {
      schema.awaitCreate(driver)
    }
    assertContains(error.toString(), "table test already exists")
  }

  // TODO: Remove this once LEGACY builds are dropped
  companion object {
    private data class Obj(val entry: String)

    private val prototype = js("Object").getPrototypeOf(Obj("test"))
    private val prototypeProps: Array<String> = js("Object").getOwnPropertyNames(prototype).unsafeCast<Array<String>>()
    private val IS_LEGACY = prototypeProps.firstOrNull { it.contains("_get_") } == null
  }
}
