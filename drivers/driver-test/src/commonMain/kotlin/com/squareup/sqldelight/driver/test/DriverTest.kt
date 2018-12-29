package com.squareup.sqldelight.driver.test

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXECUTE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
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
      db.prepareStatement(0,
          """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(), EXECUTE, 0
      )
          .execute()
      db.prepareStatement(1,
          """
              |CREATE TABLE nullability_test (
              |  id INTEGER PRIMARY KEY,
              |  integer_value INTEGER,
              |  text_value TEXT,
              |  blob_value BLOB,
              |  real_value REAL
              |);
            """.trimMargin(), EXECUTE, 0
      )
          .execute()
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
    val createInsert = {
      database.prepareStatement(2, "INSERT INTO test VALUES (?, ?);", INSERT, 2)
    }
    val createQuery = {
      database.prepareStatement(3, "SELECT * FROM test", SELECT, 0)
    }
    val createChanges = {
      database.prepareStatement(4, "SELECT changes()", SELECT, 0)
    }

    var query = createQuery()
    query.executeQuery().use {
      assertFalse(it.next())
    }

    var insert = createInsert()
    insert.bindLong(1, 1)
    insert.bindString(2, "Alec")
    insert.execute()

    query = createQuery()
    query.executeQuery().use {
      assertTrue(it.next())
      assertFalse(it.next())
    }

    var changes = createChanges()
    assertEquals(1, changes.executeQuery().apply { next() }.use { it.getLong(0) })

    query = createQuery()
    query.executeQuery().use {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
    }

    insert = createInsert()
    insert.bindLong(1, 2)
    insert.bindString(2, "Jake")
    insert.execute()
    changes = createChanges()
    assertEquals(1, changes.executeQuery().apply { next() }.use { it.getLong(0) })

    query = createQuery()
    query.executeQuery().use {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    val delete = database.prepareStatement(5, "DELETE FROM test", DELETE, 0)
    delete.execute()
    changes = createChanges()
    assertEquals(2, changes.executeQuery().apply { next() }.use { it.getLong(0) })

    query = createQuery()
    query.executeQuery().use {
      assertFalse(it.next())
    }
  }

  @Test fun `query can run multiple times`() {
    val createInsert = {
      database.prepareStatement(2, "INSERT INTO test VALUES (?, ?);", INSERT, 2)
    }
    val createChanges = {
      database.prepareStatement(4, "SELECT changes()", SELECT, 0)
    }

    var insert = createInsert()
    insert.bindLong(1, 1)
    insert.bindString(2, "Alec")
    insert.execute()
    assertEquals(1, createChanges().executeQuery().apply { next() }.use { it.getLong(0) })
    insert = createInsert()
    insert.bindLong(1, 2)
    insert.bindString(2, "Jake")
    insert.execute()
    assertEquals(1, createChanges().executeQuery().apply { next() }.use { it.getLong(0) })

    val createQuery = {
      database.prepareStatement(6, "SELECT * FROM test WHERE value = ?", SELECT, 1)
    }
    var query = createQuery()
    query.bindString(1, "Jake")

    query.executeQuery().use {
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    query = createQuery()
    query.bindString(1, "Jake")
    // Second time running the query is fine
    query.executeQuery().use {
      assertTrue(it.next())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }
  }

  @Test fun `SqlResultSet getters return null if the column values are NULL`() {
    val insert = database
        .prepareStatement(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", INSERT, 5)
    val changes = database.prepareStatement(4, "SELECT changes()", SELECT, 0)
    insert.bindLong(1, 1)
    insert.bindLong(2, null)
    insert.bindString(3, null)
    insert.bindBytes(4, null)
    insert.bindDouble(5, null)
    insert.execute()
    assertEquals(1, changes.executeQuery().apply { next() }.use { it.getLong(0) })

    val query = database
        .prepareStatement(8, "SELECT * FROM nullability_test", SELECT, 0)
    query.executeQuery().use {
      assertTrue(it.next())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
  }
}
