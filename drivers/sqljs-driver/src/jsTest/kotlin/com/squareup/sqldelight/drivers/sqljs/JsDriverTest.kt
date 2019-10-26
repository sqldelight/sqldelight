package com.squareup.sqldelight.drivers.sqljs

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.test.*

class JsDriverTest : CoroutineScope by GlobalScope {
    protected lateinit var driver: SqlDriver
    protected val schema = object : SqlDriver.Schema {
        override val version: Int = 1

        override fun create(driver: SqlDriver) {
            driver.execute(0, """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
            """.trimMargin(), 0
            )
            driver.execute(1, """
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
            driver: SqlDriver,
            oldVersion: Int,
            newVersion: Int
        ) {
            // No-op.
        }
    }

    suspend fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
        val sql = initSql()
        val db = sql.Database()
        val driver = JsSqlDriver(db)
        schema.create(driver)
        return driver
    }

    suspend fun setup() {
        driver = setupDatabase(schema = schema)
    }

    fun tearDown() {
        driver.close()
    }

    @JsName("insert_can_run_multiple_times")
    @Test fun `insert can run multiple times`() = runTest {
        setup()

        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
        }
        val query = {
            driver.executeQuery(3, "SELECT * FROM test", 0)
        }
        val changes = {
            driver.executeQuery(4, "SELECT changes()", 0)
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

        driver.execute(5, "DELETE FROM test", 0)
        assertEquals(2, changes().apply { next() }.use { it.getLong(0) })

        query().use {
            assertFalse(it.next())
        }

        tearDown()
    }

    @JsName("query_can_run_multiple_times")
    @Test fun `query can run multiple times`() = runTest {
        setup()

        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
        }
        val changes = {
            driver.executeQuery(4, "SELECT changes()", 0)
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
            driver.executeQuery(6, "SELECT * FROM test WHERE value = ?", 1, binders)
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

        tearDown()
    }

    @JsName("SqlResultSet_getters_return_null_if_the_column_values_are_NULL")
    @Test fun `SqlResultSet getters return null if the column values are NULL`() = runTest {
        setup()

        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
        }
        val changes = { driver.executeQuery(4, "SELECT changes()", 0) }
        insert {
            bindLong(1, 1)
            bindLong(2, null)
            bindString(3, null)
            bindBytes(4, null)
            bindDouble(5, null)
        }
        assertEquals(1, changes().apply { next() }.use { it.getLong(0) })

        driver.executeQuery(8, "SELECT * FROM nullability_test", 0).use {
            assertTrue(it.next())
            assertEquals(1, it.getLong(0))
            assertNull(it.getLong(1))
            assertNull(it.getString(2))
            assertNull(it.getBytes(3))
            assertNull(it.getDouble(4))
        }

        tearDown()
    }

    @JsName("types_are_correctly_converted_from_JS_to_Kotlin_and_back")
    @Test fun `types are correctly converted from JS to Kotlin and back`() = runTest {
        setup()

        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
        }

        insert {
            bindLong(1, 1)
            bindLong(2, Long.MAX_VALUE)
            bindString(3, "Hello")
            bindBytes(4, ByteArray(5) { it.toByte() })
            bindDouble(5, Float.MAX_VALUE.toDouble())
        }

        driver.executeQuery(8, "SELECT * FROM nullability_test", 0).use {
            assertTrue(it.next())
            assertEquals(1, it.getLong(0))
            assertEquals(Long.MAX_VALUE, it.getLong(1))
            assertEquals("Hello", it.getString(2))
            it.getBytes(3)?.forEachIndexed { index, byte -> assertEquals(index.toByte(), byte) }
            assertEquals(Float.MAX_VALUE.toDouble(), it.getDouble(4))
        }

        tearDown()
    }
}
