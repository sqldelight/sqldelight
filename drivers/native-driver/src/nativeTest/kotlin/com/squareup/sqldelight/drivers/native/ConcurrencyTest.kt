package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.native.concurrent.AtomicInt
import kotlin.test.*

class ConcurrencyTest {

    @Test
    fun multiRead(){
        val queryCount = AtomicInt(0)
        val ops = ThreadOperations {}
        repeat(200){
            ops.exe {
                assertEquals(countRows(), 0)
                queryCount.increment()
            }
        }

        ops.run(10)

        val transacter: TransacterImpl = object : TransacterImpl(driver) {}

        transacter.transaction {
            insertTestData(TestData(1234L, "arst"))
            while (queryCount.value != 200){
                println("queryCount.value ${queryCount.value}")
                sleep(500)
            }
        }

        assertEquals(countRows(), 1)
    }

    fun countRows():Long{
        val cur = driver.executeQuery(0, "SELECT count(*) FROM test", 0)
        try {
            cur.next()
            val count = cur.getLong(0)
            return count!!
        } finally {
            cur.close()
        }
    }

    /*@Test
    fun executeAsOne() {
        val data1 = TestData(1, "val1")
        insertTestData(data1)

        assertNotEquals(data1, testDataQuery().executeAsOne())
    }*/

    private val mapper = { cursor: SqlCursor ->
        TestData(
            cursor.getLong(0)!!, cursor.getString(1)!!
        )
    }

    private lateinit var driver: SqlDriver

    fun setupDatabase(schema: SqlDriver.Schema): SqlDriver {
        val name = "testdb"
        DatabaseFileContext.deleteDatabase(name)
        return NativeSqliteDriver(schema, name, maxConcurrentReader = 4)
    }

    @BeforeTest
    fun setup() {
        driver = setupDatabase(
            schema = object : SqlDriver.Schema {
                override val version: Int = 1

                override fun create(driver: SqlDriver) {
                    driver.execute(
                        null,
                        """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
                        0
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
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
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