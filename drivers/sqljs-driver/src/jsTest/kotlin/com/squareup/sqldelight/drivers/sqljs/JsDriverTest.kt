package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.sqljs.initSqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class JsDriverTest {

  private fun testing(action: suspend CoroutineScope.(SqlDriver) -> Unit) = runTest {
    val driver = initSqlDriver().await()
    schema.create(driver)
    action(driver)
    driver.close()
  }

  private val schema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(
        0,
        """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
        """.trimMargin(),
        0,
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
        0,
      )
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
      // No-op.
      return QueryResult.Unit
    }
  }

  @Test fun insert_can_run_multiple_times() = testing { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }
    fun query(mapper: (SqlCursor) -> QueryResult<Unit>) {
      driver.executeQuery(3, "SELECT * FROM test", mapper, 0)
    }
    fun changes(mapper: (SqlCursor) -> QueryResult<Long?>): Long? {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0).value
    }

    query {
      assertFalse(it.next().value)
      QueryResult.Unit
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
    }

    query {
      assertTrue(it.next().value)
      assertFalse(it.next().value)
      QueryResult.Unit
    }

    assertEquals(1, changes { it.next(); QueryResult.Value(it.getLong(0)) })

    query {
      assertTrue(it.next().value)
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      QueryResult.Unit
    }

    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(1, changes { it.next(); QueryResult.Value(it.getLong(0)) })

    query {
      assertTrue(it.next().value)
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next().value)
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
      QueryResult.Unit
    }

    driver.execute(5, "DELETE FROM test", 0)
    assertEquals(2, changes { it.next(); QueryResult.Value(it.getLong(0)) })

    query {
      assertFalse(it.next().value)
      QueryResult.Unit
    }
  }

  @Test fun query_can_run_multiple_times() = testing { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }
    fun changes(mapper: (SqlCursor) -> QueryResult<Long?>): Long? {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0).value
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
    }
    assertEquals(1, changes { it.next(); QueryResult.Value(it.getLong(0)) })
    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(1, changes { it.next(); QueryResult.Value(it.getLong(0)) })

    fun query(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> QueryResult<Unit>) {
      driver.executeQuery(6, "SELECT * FROM test WHERE value = ?", mapper, 1, binders)
    }
    query(
      binders = {
        bindString(0, "Jake")
      },
      mapper = {
        assertTrue(it.next().value)
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
        QueryResult.Unit
      },
    )

    // Second time running the query is fine
    query(
      binders = {
        bindString(0, "Jake")
      },
      mapper = {
        assertTrue(it.next().value)
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
        QueryResult.Unit
      },
    )
  }

  @Test fun sqlResultSet_getters_return_null_if_the_column_values_are_NULL() = testing { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }
    fun changes(mapper: (SqlCursor) -> QueryResult<Long?>): Long? {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0).value
    }

    insert {
      bindLong(0, 1)
      bindLong(1, null)
      bindString(2, null)
      bindBytes(3, null)
      bindDouble(4, null)
    }
    assertEquals(1, changes { it.next(); QueryResult.Value(it.getLong(0)) })

    val mapper: (SqlCursor) -> QueryResult<Unit> = {
      assertTrue(it.next().value)
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
      QueryResult.Unit
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0)
  }

  @Test fun types_are_correctly_converted_from_JS_to_Kotlin_and_back() = testing { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }

    insert {
      bindLong(0, 1)
      bindLong(1, Long.MAX_VALUE)
      bindString(2, "Hello")
      bindBytes(3, ByteArray(5) { it.toByte() })
      bindDouble(4, Float.MAX_VALUE.toDouble())
    }

    val mapper: (SqlCursor) -> QueryResult<Unit> = {
      assertTrue(it.next().value)
      assertEquals(1, it.getLong(0))
      assertEquals(Long.MAX_VALUE, it.getLong(1))
      assertEquals("Hello", it.getString(2))
      it.getBytes(3)?.forEachIndexed { index, byte -> assertEquals(index.toByte(), byte) }
      assertEquals(Float.MAX_VALUE.toDouble(), it.getDouble(4))
      QueryResult.Unit
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0)
  }
}
