package app.cash.sqldelight.async

import app.cash.sqldelight.async.SqlDriverAdapter.Companion.asAsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlCursor
import app.cash.sqldelight.async.db.AsyncSqlDriver
import app.cash.sqldelight.async.db.AsyncSqlPreparedStatement
import app.cash.sqldelight.async.db.AsyncSqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

typealias InsertFunction = suspend (AsyncSqlPreparedStatement.() -> Unit) -> Unit

class SqlDriverAdapterTest {
  private val schema = object : AsyncSqlSchema {
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
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    block(driver.asAsyncSqlDriver().also { schema.create(it) })
    driver.close()
  }

  @Test
  fun `insert can run multiple times`() = runTest { driver ->

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
  fun `query can run multiple times`() = runTest { driver ->

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
}
