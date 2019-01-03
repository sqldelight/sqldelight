package com.squareup.sqldelight.driver.test

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.use
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class DriverTest {
  protected lateinit var database: SqlDatabase
  protected val schema = object : Schema {
    override val version: Int = 1

    override fun create(db: SqlDatabase) {
      db.execute(0, """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(), 0
      )
      db.execute(1, """
              |CREATE TABLE nullability_test (
              |  id INTEGER PRIMARY KEY,
              |  integer_value INTEGER,
              |  text_value TEXT,
              |  blob_value BLOB,
              |  real_value REAL
              |);
            """.trimMargin(), 0
      )
    }

    override fun migrate(
      db: SqlDatabase,
      oldVersion: Int,
      newVersion: Int
    ) {
      // No-op.
    }
  }

  abstract fun setupDatabase(schema: Schema): SqlDatabase

  @BeforeTest fun setup() {
    database = setupDatabase(schema = schema)
  }

  @AfterTest fun tearDown() {
    database.close()
  }

  @Test fun `insert can run multiple times`() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      database.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }
    val query = {
      database.executeQuery(3, "SELECT * FROM test", 0)
    }
    val changes = {
      database.executeQuery(4, "SELECT changes()", 0)
    }

    query().use {
      assertFalse(it.next())
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }

    query().use {
      assertTrue(it.next())
      assertFalse(it.next())
    }

    assertEquals(1, changes().apply { next() }.use { it.getLong(0) })

    query().use {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
    }

    insert {
      bindLong(1, 2)
      bindString(2, "Jake")
    }
    assertEquals(1, changes().apply { next() }.use { it.getLong(0) })

    query().use {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    database.execute(5, "DELETE FROM test", 0)
    assertEquals(2, changes().apply { next() }.use { it.getLong(0) })

    query().use {
      assertFalse(it.next())
    }
  }

  @Test fun `query can run multiple times`() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      database.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }
    val changes = {
      database.executeQuery(4, "SELECT changes()", 0)
    }

    insert {
      bindLong(1, 1)
      bindString(2, "Alec")
    }
    assertEquals(1, changes().apply { next() }.use { it.getLong(0) })
    insert {
      bindLong(1, 2)
      bindString(2, "Jake")
    }
    assertEquals(1, changes().apply { next() }.use { it.getLong(0) })

    val query = { binders: SqlPreparedStatement.() -> Unit ->
      database.executeQuery(6, "SELECT * FROM test WHERE value = ?", 1, binders)
    }
    query {
      bindString(1, "Jake")
    }.use {
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    // Second time running the query is fine
    query {
      bindString(1, "Jake")
    }.use {
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }
  }

  @Test fun `SqlResultSet getters return null if the column values are NULL`() {
    val insert = { binders: SqlPreparedStatement.() -> Unit ->
      database.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }
    val changes = { database.executeQuery(4, "SELECT changes()", 0) }
    insert {
      bindLong(1, 1)
      bindLong(2, null)
      bindString(3, null)
      bindBytes(4, null)
      bindDouble(5, null)
    }
    assertEquals(1, changes().apply { next() }.use { it.getLong(0) })

    database.executeQuery(8, "SELECT * FROM nullability_test", 0).use {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
  }
}
