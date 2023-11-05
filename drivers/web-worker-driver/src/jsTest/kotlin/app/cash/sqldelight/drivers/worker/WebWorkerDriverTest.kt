package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitQuery
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import app.cash.sqldelight.driver.worker.WebWorkerException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.w3c.dom.Worker

typealias InsertFunction = suspend (SqlPreparedStatement.() -> Unit) -> Unit

@OptIn(ExperimentalCoroutinesApi::class)
class WebWorkerDriverTest {
  private val schema = object : SqlSchema<AsyncValue<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
      driver.execute(
        0,
        """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
        """.trimMargin(),
        0,
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
        0,
      ).await()
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ) = QueryResult.AsyncValue {}
  }

  private fun runTest(block: suspend (SqlDriver) -> Unit) = kotlinx.coroutines.test.runTest {
    @Suppress("UnsafeCastFromDynamic")
    val driver = WebWorkerDriver(Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")))
      .also { schema.awaitCreate(it) }
    block(driver)
    driver.close()
  }

  @Test
  fun insert_can_run_multiple_times() = runTest { driver ->

    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    suspend fun query(mapper: suspend (SqlCursor) -> Unit) {
      driver.awaitQuery(3, "SELECT * FROM test", mapper, 0)
    }

    suspend fun changes(mapper: suspend (SqlCursor) -> Long?): Long? {
      return driver.awaitQuery(4, "SELECT changes()", mapper, 0)
    }

    query {
      assertFalse(it.next().await())
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
    }

    query {
      assertTrue(it.next().await())
      assertFalse(it.next().await())
    }

    assertEquals(
      1,
      changes {
        it.next()
        it.getLong(0)
      },
    )

    query {
      assertTrue(it.next().await())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
    }

    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(
      1,
      changes {
        it.next()
        it.getLong(0)
      },
    )

    query {
      assertTrue(it.next().await())
      assertEquals(1, it.getLong(0))
      assertEquals("Alec", it.getString(1))
      assertTrue(it.next().await())
      assertEquals(2, it.getLong(0))
      assertEquals("Jake", it.getString(1))
    }

    driver.await(5, "DELETE FROM test", 0)
    assertEquals(
      2,
      changes {
        it.next()
        it.getLong(0)
      },
    )

    query {
      assertFalse(it.next().await())
    }
  }

  @Test
  fun query_can_run_multiple_times() = runTest { driver ->

    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
    }

    suspend fun changes(mapper: (SqlCursor) -> Long?): Long? {
      return driver.awaitQuery(4, "SELECT changes()", mapper, 0)
    }

    insert {
      bindLong(0, 1)
      bindString(1, "Alec")
    }
    assertEquals(
      1,
      changes {
        it.next()
        it.getLong(0)
      },
    )
    insert {
      bindLong(0, 2)
      bindString(1, "Jake")
    }
    assertEquals(
      1,
      changes {
        it.next()
        it.getLong(0)
      },
    )

    suspend fun query(binders: SqlPreparedStatement.() -> Unit, mapper: suspend (SqlCursor) -> Unit) {
      driver.awaitQuery(6, "SELECT * FROM test WHERE value = ?", mapper, 1, binders)
    }
    query(
      binders = {
        bindString(0, "Jake")
      },
      mapper = {
        assertTrue(it.next().await())
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
        assertTrue(it.next().await())
        assertEquals(2, it.getLong(0))
        assertEquals("Jake", it.getString(1))
      },
    )
  }

  @Test
  fun sqlResultSet_getters_return_null_if_the_column_values_are_NULL() = runTest { driver ->
    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }

    suspend fun changes(mapper: (SqlCursor) -> Long?): Long? {
      return driver.awaitQuery(4, "SELECT changes()", mapper, 0)
    }

    val inserted = insert {
      bindLong(0, 1)
      bindLong(1, null)
      bindString(2, null)
      bindBytes(3, null)
      bindDouble(4, null)
    }

    val mapper: suspend (SqlCursor) -> Unit = {
      assertTrue(it.next().await())
      assertEquals(1, it.getLong(0))
      assertNull(it.getLong(1))
      assertNull(it.getString(2))
      assertNull(it.getBytes(3))
      assertNull(it.getDouble(4))
    }
    driver.awaitQuery(8, "SELECT * FROM nullability_test", mapper, 0)
    changes {
      it.next()
      it.getLong(0)
    }
  }

  @Test
  fun types_are_correctly_converted_from_JS_to_Kotlin_and_back() = runTest { driver ->
    val insert: InsertFunction = { binders: SqlPreparedStatement.() -> Unit ->
      driver.await(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
    }

    insert {
      bindLong(0, 1)
      bindLong(1, Long.MAX_VALUE)
      bindString(2, "Hello")
      bindBytes(3, ByteArray(5) { it.toByte() })
      bindDouble(4, Float.MAX_VALUE.toDouble())
    }

    val mapper: suspend (SqlCursor) -> Unit = {
      assertTrue(it.next().await())
      assertEquals(1, it.getLong(0))
      assertEquals(Long.MAX_VALUE, it.getLong(1))
      assertEquals("Hello", it.getString(2))
      it.getBytes(3)?.forEachIndexed { index, byte -> assertEquals(index.toByte(), byte) }
      assertEquals(Float.MAX_VALUE.toDouble(), it.getDouble(4))
    }
    driver.awaitQuery(8, "SELECT * FROM nullability_test", mapper, 0)
  }

  @Test
  fun worker_exceptions_are_handled_correctly() = runTest { driver ->
    val error = assertFailsWith<WebWorkerException> {
      schema.awaitCreate(driver)
    }
    assertContains(error.toString(), "table test already exists")
  }

  @Test
  fun bad_worker_results_values_throws_error() = kotlinx.coroutines.test.runTest {
    val exception = assertFailsWith<IllegalStateException> {
      @Suppress("UnsafeCastFromDynamic")
      val driver = WebWorkerDriver(Worker(js("""new URL("./bad.worker.js", import.meta.url)""")))
        .also { schema.awaitCreate(it) }
      driver.close()
    }

    assertEquals(exception.message, "The worker result values were not an array")
  }
}
