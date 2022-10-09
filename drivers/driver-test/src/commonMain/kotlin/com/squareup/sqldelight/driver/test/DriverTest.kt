package com.squareup.sqldelight.driver.test

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.internal.Atomic
import app.cash.sqldelight.internal.getValue
import app.cash.sqldelight.internal.setValue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class DriverTest {
  protected lateinit var driver: SqlDriver
  protected val schema = object : SqlSchema {
    override val version: Int = 1

    override fun create(driver: SqlDriver): QueryResult<Unit> {
      driver.execute(
        0,
        """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
        """.trimMargin(),
        emptyList(),
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
        emptyList(),
      )
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Int,
      newVersion: Int,
    ) = QueryResult.Unit
  }
  private var transacter by Atomic<Transacter?>(null)

  abstract fun setupDatabase(schema: SqlSchema): SqlDriver

  private fun changes(): Long? {
    // wrap in a transaction to ensure read happens on transaction thread/connection
    return transacter!!.transactionWithResult {
      val mapper: (SqlCursor) -> Long? = {
        it.next()
        it.getLong(0)
      }
      driver.executeQuery(null, "SELECT changes()", mapper, emptyList()).value
    }
  }

  @BeforeTest fun setup() {
    driver = setupDatabase(schema = schema)
    transacter = object : TransacterImpl(driver) {}
  }

  @AfterTest fun tearDown() {
    transacter = null
    driver.close()
  }

  @Test
  fun insertCanRunMultipleTimes() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", listOf(25, 28), binders)
    }
    fun query(mapper: (SqlCursor) -> Unit) {
      driver.executeQuery(3, "SELECT * FROM test", mapper, emptyList())
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

    assertEquals(1, changes())

    query {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
    }

    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(1, changes())

    query {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    driver.execute(5, "DELETE FROM test", emptyList())
    assertEquals(2, changes())

    query {
      assertFalse(it.next())
    }
  }

  @Test
  fun queryCanRunMultipleTimes() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(2, "INSERT INTO test VALUES (?, ?);", listOf(25, 28), binders)
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
    }
    assertEquals(1, changes())
    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(1, changes())

    fun query(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> Unit) {
      driver.executeQuery(6, "SELECT * FROM test WHERE value = ?", mapper, listOf(33), binders)
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

  @Test fun sqlResultSetGettersReturnNullIfTheColumnValuesAreNULL() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", listOf(37, 40, 43, 46, 49), binders)
    }
    insert {
      bindLong(0, 1)
      bindLong(1, null)
      bindString(2, null)
      bindBytes(3, null)
      bindDouble(4, null)
    }
    assertEquals(1, changes())

    val mapper: (SqlCursor) -> Unit = {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
    driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, emptyList())
  }
}
