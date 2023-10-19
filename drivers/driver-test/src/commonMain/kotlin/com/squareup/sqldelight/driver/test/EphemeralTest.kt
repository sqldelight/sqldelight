package com.squareup.sqldelight.driver.test

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test for SQLite ephemeral database configurations
 * */
abstract class EphemeralTest {

  protected enum class Type {
    IN_MEMORY,
    NAMED,
    TEMPORARY,
  }

  protected open val skipNamed: Boolean = false

  protected val schema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(
        null,
        """
          CREATE TABLE test (
            id INTEGER NOT NULL PRIMARY KEY,
            value TEXT NOT NULL
           );
        """.trimIndent(),
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

  private val mapper = { cursor: SqlCursor ->
    TestData(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
    )
  }

  protected abstract fun setupDatabase(type: Type): SqlDriver

  @Test
  fun inMemoryCreatesIndependentDatabase() {
    val data1 = TestData(1, "val1")
    val driver1 = setupDatabase(Type.IN_MEMORY)
    driver1.insertTestData(data1)
    assertEquals(data1, driver1.testDataQuery().executeAsOne())

    val driver2 = setupDatabase(Type.IN_MEMORY)
    assertNull(driver2.testDataQuery().executeAsOneOrNull())
    driver1.close()
    driver2.close()
  }

  @Test
  fun temporaryCreatesIndependentDatabase() {
    val data1 = TestData(1, "val1")
    val driver1 = setupDatabase(Type.TEMPORARY)
    driver1.insertTestData(data1)
    assertEquals(data1, driver1.testDataQuery().executeAsOne())

    val driver2 = setupDatabase(Type.TEMPORARY)
    assertNull(driver2.testDataQuery().executeAsOneOrNull())
    driver1.close()
    driver2.close()
  }

  @Test
  fun namedCreatesSharedDatabase() {
    if (skipNamed) return

    val data1 = TestData(1, "val1")
    val driver1 = setupDatabase(Type.NAMED)
    driver1.insertTestData(data1)
    assertEquals(data1, driver1.testDataQuery().executeAsOne())

    val driver2 = setupDatabase(Type.NAMED)
    assertEquals(data1, driver2.testDataQuery().executeAsOne())
    driver1.close()
    assertEquals(data1, driver2.testDataQuery().executeAsOne())
    driver2.close()

    val driver3 = setupDatabase(Type.NAMED)
    assertNull(driver3.testDataQuery().executeAsOneOrNull())
    driver3.close()
  }

  private fun SqlDriver.insertTestData(testData: TestData) {
    execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
      bindLong(0, testData.id)
      bindString(1, testData.value)
    }
  }

  private fun SqlDriver.testDataQuery(): Query<TestData> {
    return object : Query<TestData>(mapper) {
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return executeQuery(0, "SELECT * FROM test", mapper, 0, null)
      }

      override fun addListener(listener: Listener) {
        addListener("test", listener = listener)
      }

      override fun removeListener(listener: Listener) {
        removeListener("test", listener = listener)
      }
    }
  }

  private data class TestData(val id: Long, val value: String)
}
