package com.squareup.sqldelight.driver

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXECUTE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.use
import org.junit.After
import org.junit.Before
import org.junit.Test

abstract class DriverTest {
  private lateinit var database: SqlDatabase

  @Before fun setup() {
    database = setupDatabase(
        schema = object : SqlDatabase.Schema {
          override val version: Int = 1

          override fun create(db: SqlDatabaseConnection) {
            db.prepareStatement("""
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(), EXECUTE, 0).execute()
            db.prepareStatement("""
              |CREATE TABLE nullability_test (
              |  id INTEGER PRIMARY KEY,
              |  integer_value INTEGER,
              |  text_value TEXT,
              |  blob_value BLOB,
              |  real_value REAL
              |);
            """.trimMargin(), EXECUTE, 0).execute()
          }

          override fun migrate(
            db: SqlDatabaseConnection,
            oldVersion: Int,
            newVersion: Int
          ) {
            // No-op.
          }
        }
    )
  }

  @After fun tearDown() {
    database.close()
  }

  abstract fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase

  @Test fun `insert can run multiple times`() {
    val insert = database.getConnection().prepareStatement("INSERT INTO test VALUES (?, ?);", INSERT, 2)
    val query = database.getConnection().prepareStatement("SELECT * FROM test", SELECT, 0)
    val changes = database.getConnection().prepareStatement("SELECT changes()", SELECT, 0)

    query.executeQuery().use {
      assertThat(it.next()).isFalse()
    }

    insert.bindLong(1, 1)
    insert.bindString(2, "Alec")
    insert.execute()
    assertThat(changes.executeQuery().apply { next() }.getLong(0)).isEqualTo(1)

    query.executeQuery().use {
      assertThat(it.next()).isTrue()
      assertThat(it.getLong(0)).isEqualTo(1)
      assertThat(it.getString(1)).isEqualTo("Alec")
    }

    insert.bindLong(1, 2)
    insert.bindString(2, "Jake")
    insert.execute()
    assertThat(changes.executeQuery().apply { next() }.getLong(0)).isEqualTo(1)

    query.executeQuery().use {
      assertThat(it.next()).isTrue()
      assertThat(it.getLong(0)).isEqualTo(1)
      assertThat(it.getString(1)).isEqualTo("Alec")
      assertThat(it.next()).isTrue()
      assertThat(it.getLong(0)).isEqualTo(2)
      assertThat(it.getString(1)).isEqualTo("Jake")
    }

    val delete = database.getConnection().prepareStatement("DELETE FROM test", DELETE, 0)
    delete.execute()
    assertThat(changes.executeQuery().apply { next() }.getLong(0)).isEqualTo(2)

    query.executeQuery().use {
      assertThat(it.next()).isFalse()
    }
  }

  @Test fun `query can run multiple times`() {
    val insert = database.getConnection().prepareStatement("INSERT INTO test VALUES (?, ?);", INSERT, 2)
    val changes = database.getConnection().prepareStatement("SELECT changes()", SELECT, 0)
    insert.bindLong(1, 1)
    insert.bindString(2, "Alec")
    insert.execute()
    assertThat(changes.executeQuery().apply { next() }.getLong(0)).isEqualTo(1)
    insert.bindLong(1, 2)
    insert.bindString(2, "Jake")
    insert.execute()
    assertThat(changes.executeQuery().apply { next() }.getLong(0)).isEqualTo(1)


    val query = database.getConnection().prepareStatement("SELECT * FROM test WHERE value = ?", SELECT, 1)
    query.bindString(1, "Jake")

    query.executeQuery().use {
      assertThat(it.next()).isTrue()
      assertThat(it.getLong(0)).isEqualTo(2)
      assertThat(it.getString(1)).isEqualTo("Jake")
    }

    // Second time running the query is fine
    query.executeQuery().use {
      assertThat(it.next()).isTrue()
      assertThat(it.getLong(0)).isEqualTo(2)
      assertThat(it.getString(1)).isEqualTo("Jake")
    }
  }

  @Test fun `SqlResultSet getters return null if the column values are NULL`() {
    val insert = database.getConnection().prepareStatement("INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", INSERT, 5)
    val changes = database.getConnection().prepareStatement("SELECT changes()", SELECT, 0)
    insert.bindLong(1, 1)
    insert.bindLong(2, null)
    insert.bindString(3, null)
    insert.bindBytes(4, null)
    insert.bindDouble(5, null)
    insert.execute()
    assertThat(changes.executeQuery().apply { next() }.getLong(0)).isEqualTo(1)

    val query = database.getConnection().prepareStatement("SELECT * FROM nullability_test", SELECT, 0)
    query.executeQuery().use {
      assertThat(it.next()).isTrue()
      assertThat(it.getLong(0)).isEqualTo(1)
      assertThat(it.getLong(1)).isNull()
      assertThat(it.getString(2)).isNull()
      assertThat(it.getBytes(3)).isNull()
      assertThat(it.getDouble(4)).isNull()
    }
  }
}
