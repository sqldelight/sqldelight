package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.currentTimeMillis
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.test.*

class ConcurrencyTest {

    @Test
    fun writeNotBlockRead() {
        assertEquals(countRows(), 0)

        val transacter: TransacterImpl = object : TransacterImpl(driver) {}
        val worker = Worker.start()
        val counter = AtomicInt(0)
        val transactionStarted = AtomicInt(0)

        val block = {
            transacter.transaction {
                insertTestData(TestData(1L, "arst 1"))
                transactionStarted.increment()
                sleep(1500)
                counter.increment()
            }
        }

        val future = worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }

        waitFor { transactionStarted.value > 0 }

        assertEquals(counter.value, 0)
        assertEquals(0L, countRows())
        assertEquals(counter.value, 0)

        future.result
    }


    @Test
    fun writeBlocksWrite() {
        val transacter: TransacterImpl = object : TransacterImpl(driver) {}
        val worker = Worker.start()
        val counter = AtomicInt(0)
        val transactionStarted = AtomicInt(0)

        val block = {
            transacter.transaction {
                insertTestData(TestData(1L, "arst 1"))
                transactionStarted.increment()
                sleep(1500)
                counter.increment()
            }
        }

        val future = worker.execute(TransferMode.SAFE, { block.freeze() }) { it() }

        waitFor { transactionStarted.value > 0 }

        assertEquals(counter.value, 0)
        insertTestData(TestData(2L, "arst 2"))
        assertEquals(counter.value, 1)

        future.result
    }

    private fun waitFor(timeout:Long = 10_000, block:()->Boolean){
        val start = currentTimeMillis()
        var wasTimeout = false

        while (!block() && !wasTimeout){
            sleep(200)
            wasTimeout = (currentTimeMillis() - start) > timeout
        }

        if(wasTimeout)
            throw IllegalStateException("Timeout $timeout exceeded")
    }


    @Test
    fun multiWrite() {
        val ops = ThreadOperations {}
        val times = 10_000
        val transacter: TransacterImpl = object : TransacterImpl(driver) {}

        repeat(times) { index ->
            ops.exe {
                transacter.transaction {
                    insertTestData(TestData(index.toLong(), "arst $index"))

                    val id2 = index.toLong() + times
                    insertTestData(TestData(id2, "arst $id2"))

                    val id3 = index.toLong() + times + times
                    insertTestData(TestData(id3, "arst $id3"))
                }
            }
        }

        ops.run(10)

        assertEquals(countRows(), times.toLong() * 3)

        /*val workers = Array(10) { Worker.start(name = "Worker $it") }
        workers.forEach {
            it.execute(TransferMode.SAFE, {}) {

            }
        }*/
    }

    @Test
    fun multiRead() {
        assertEquals(countRows(), 0)

        val start = currentTimeMillis()
        val queryCount = AtomicInt(0)
        val ops = ThreadOperations {}
        val runs = 200
        repeat(runs) {
            ops.exe {
                assertEquals(countRows(), 0)
                queryCount.increment()
            }
        }

        ops.run(10)

        val transacter: TransacterImpl = object : TransacterImpl(driver) {}

        transacter.transaction {
            insertTestData(TestData(1234L, "arst"))
            var wasTimeout = false
            while (queryCount.value != runs && !wasTimeout) {
                println("queryCount.value ${queryCount.value}")
                sleep(500)
                wasTimeout = (currentTimeMillis() - start) > 5000
            }

            assertFalse(wasTimeout, "Test timed out")
        }

        assertEquals(countRows(), 1)
    }

    fun countRows(): Long {
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