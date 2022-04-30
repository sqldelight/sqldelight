package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.db.AsyncSqlDriver
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.combine
import app.cash.sqldelight.driver.sqljs.asPromise
import app.cash.sqldelight.driver.sqljs.initAsyncSqlDriver
import kotlin.js.Promise
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsWorkerDriverTest {

  private lateinit var driverPromise: Promise<AsyncSqlDriver>
  private val schema = object : AsyncSqlDriver.Schema {
    override val version: Int = 1

    override fun create(driver: AsyncSqlDriver): AsyncSqlDriver.Callback<Unit> {
      return listOf(
        driver.execute(
          0,
          """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(),
          0
        ),
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
      ).combine()
    }

    override fun migrate(driver: AsyncSqlDriver, oldVersion: Int, newVersion: Int): AsyncSqlDriver.Callback<Unit> {
      // No-op.
      return emptyList<AsyncSqlDriver.Callback<Unit>>().combine()
    }
  }

  @BeforeTest
  fun setup() {
    driverPromise = initAsyncSqlDriver("/worker.sql-wasm.js").then {
      schema.create(it)
      it
    }
  }

  @AfterTest
  fun tearDown() {
    driverPromise.then { it.close() }
  }

  // TODO: Implement/Test prepared statements
  /*@Test
  fun insert_can_run_multiple_times() = driverPromise.then { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    fun query(mapper: (SqlCursor) -> Unit) {
      driver.executeQuery(3, "SELECT * FROM test", mapper, 0)
    }

    fun changes(mapper: (SqlCursor) -> Long?): Long? {
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
  }*/

  /*@Test
  fun query_can_run_multiple_times() = driverPromise.then { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    fun changes(mapper: (SqlCursor) -> Long?): Long? {
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

    fun query(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> Unit) {
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
  }*/

  @Test
  fun sqlResultSet_getters_return_null_if_the_column_values_are_NULL() = driverPromise.then { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders).asPromise()
    }

    fun changes(mapper: (SqlCursor) -> Long?): Promise<Long?> {
      return driver.executeQuery(4, "SELECT changes()", mapper, 0).asPromise()
    }

    val inserted = insert {
      bindLong(1, 1)
      bindLong(2, null)
      bindString(3, null)
      bindBytes(4, null)
      bindDouble(5, null)
    }

    val mapper: (SqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
    val test = driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0).asPromise()
    Promise.all(arrayOf(inserted.then { console.log("Inserted complete!") }, test, changes { it.next(); it.getLong(0) }.then { }))
  }

  @Test
  fun types_are_correctly_converted_from_JS_to_Kotlin_and_back() = driverPromise.then { driver ->

    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders).asPromise()
    }

    val inserted = insert {
      bindLong(1, 1)
      bindLong(2, Long.MAX_VALUE)
      bindString(3, "Hello")
      bindBytes(4, ByteArray(5) { it.toByte() })
      bindDouble(5, Float.MAX_VALUE.toDouble())
    }

    val mapper: (SqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals(Long.MAX_VALUE, it.getLong(1))
      assertEquals("Hello", it.getString(2))
      it.getBytes(3)?.forEachIndexed { index, byte -> assertEquals(index.toByte(), byte) }
      assertEquals(Float.MAX_VALUE.toDouble(), it.getDouble(4))
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0).asPromise()
  }
}
