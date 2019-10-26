package com.squareup.sqldelight.drivers.sqljs

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.Atomic
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsQueryTest : CoroutineScope by GlobalScope {
    private val mapper = { cursor: SqlCursor ->
        TestData(
            cursor.getLong(0)!!, cursor.getString(1)!!
        )
    }

    private lateinit var driver: SqlDriver

    suspend fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
        val sql = initSql()
        val db = sql.Database()
        val driver = JsSqlDriver(db)
        schema.create(driver)
        return driver
    }

    suspend fun setup() {
        driver = setupDatabase(
            schema = object : SqlDriver.Schema {
                override val version: Int = 1

                override fun create(driver: SqlDriver) {
                    driver.execute(null, """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
               """.trimIndent(), 0)

                }

                override fun migrate(
                    driver: SqlDriver,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    // No-op.
                }
            }
        )
    }

    fun tearDown() {
        driver.close()
    }

    @Test fun executeAsOne() = runTest {
        setup()

        val data1 = TestData(1, "val1")
        insertTestData(data1)

        assertEquals(data1, testDataQuery().executeAsOne())

        tearDown()
    }

    @Test fun executeAsOneTwoTimes() = runTest {
        setup()

        val data1 = TestData(1, "val1")
        insertTestData(data1)

        val query = testDataQuery()

        assertEquals(query.executeAsOne(), query.executeAsOne())

        tearDown()
    }

    @Test fun executeAsOneThrowsNpeForNoRows() = runTest {
        setup()

        try {
            testDataQuery().executeAsOne()
            throw AssertionError("Expected an IllegalStateException")
        } catch (ignored: NullPointerException) {

        } finally {
            tearDown()
        }
    }

    @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() = runTest {
        setup()

        try {
            insertTestData(TestData(1, "val1"))
            insertTestData(TestData(2, "val2"))

            testDataQuery().executeAsOne()
            throw AssertionError("Expected an IllegalStateException")
        } catch (ignored: IllegalStateException) {

        } finally {
            tearDown()
        }
    }

    @Test fun executeAsOneOrNull() = runTest {
        setup()

        val data1 = TestData(1, "val1")
        insertTestData(data1)

        val query = testDataQuery()
        assertEquals(data1, query.executeAsOneOrNull())

        tearDown()
    }

    @Test fun executeAsOneOrNullReturnsNullForNoRows() = runTest {
        setup()

        assertNull(testDataQuery().executeAsOneOrNull())

        tearDown()
    }

    @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() = runTest {
        setup()

        try {
            insertTestData(TestData(1, "val1"))
            insertTestData(TestData(2, "val2"))

            testDataQuery().executeAsOneOrNull()
            throw AssertionError("Expected an IllegalStateException")
        } catch (ignored: IllegalStateException) {

        } finally {
            tearDown()
        }
    }

    @Test fun executeAsList() = runTest {
        setup()

        val data1 = TestData(1, "val1")
        val data2 = TestData(2, "val2")

        insertTestData(data1)
        insertTestData(data2)

        assertEquals(listOf(data1, data2), testDataQuery().executeAsList())

        tearDown()
    }

    @Test fun executeAsListForNoRows() = runTest {
        setup()

        assertTrue(testDataQuery().executeAsList().isEmpty())

        tearDown()
    }

    @Test fun notifyDataChangedNotifiesListeners() = runTest {
        setup()

        val notifies = Atomic(0)
        val query = testDataQuery()
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                notifies.increment()
            }
        }

        query.addListener(listener)
        assertEquals(0, notifies.get())

        query.notifyDataChanged()
        assertEquals(1, notifies.get())

        tearDown()
    }

    @Test fun removeListenerActuallyRemovesListener() = runTest {
        setup()

        val notifies = Atomic(0)
        val query = testDataQuery()
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                notifies.increment()
            }
        }

        query.addListener(listener)
        query.removeListener(listener)
        query.notifyDataChanged()
        assertEquals(0, notifies.get())

        tearDown()
    }

    private fun insertTestData(testData: TestData) {
        driver.execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
            bindLong(1, testData.id)
            bindString(2, testData.value)
        }
    }

    private fun testDataQuery(): Query<TestData> {
        return object : Query<TestData>(copyOnWriteList(), mapper) {
            override fun execute(): SqlCursor {
                return driver.executeQuery(0, "SELECT * FROM test", 0)
            }
        }
    }

    private data class TestData(val id: Long, val value: String)
}

// Not actually atomic, the type needs to be as the listeners get frozen.
private fun Atomic<Int>.increment() = set(get() + 1)
