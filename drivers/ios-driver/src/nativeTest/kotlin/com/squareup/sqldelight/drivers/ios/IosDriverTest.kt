package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.*

import co.touchlab.sqliter.NativeFileContext.deleteDatabase
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.freeze
import co.touchlab.testhelp.concurrency.*
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.driver.test.DriverTest
import kotlin.test.*

class IosDriverTest : DriverTest() {
    override fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase {
        val configuration = DatabaseConfiguration(
                name = "testdb",
                version = 1,
                create = { connection ->
                    wrapConnection(connection) {
                        schema.create(it)
                    }
                },
                busyTimeout = 20_000)
        deleteDatabase(configuration.name)
        return SqliterSqlDatabase(createDatabaseManager(configuration))
    }

    // TODO: https://github.com/JetBrains/kotlin-native/issues/2328

    @BeforeTest
    fun setup2() {
        super.setup()
    }

    @AfterTest
    fun tearDown2() {
        super.tearDown()
    }

    // Sanity check of the driver.
    @Test
    fun basicTest() {
        val cursor = database.getConnection().prepareStatement("SELECT 1", SELECT, 0).executeQuery()
        cursor.next()
        assertEquals(1, cursor.getLong(0))
        cursor.close()
    }

    @Test
    fun `insert can run multiple times2`() {
        super.`insert can run multiple times`()
    }

    @Test
    fun `query can run multiple times2`() {
        super.`query can run multiple times`()
    }

    @Test
    fun `SqlResultSet getters return null if the column values are NULL2`() {
        super.`SqlResultSet getters return null if the column values are NULL`()
    }

    @Test
    fun threadLocalBasicTest() {
        val inc = AtomicInt(0).freeze()
        val ops = ThreadOperations { ThreadLocalCache { TestData("asdf", inc.incrementAndGet()) } }
        for (i in 0 until 50) {
            ops.exe {
                it.mineOrAlign()
            }
        }

        val THREADS = 5

        val cache = ops.run(THREADS)

        assertEquals(THREADS, cache.cache.size)

        val allList = mutableListOf<TestData>()
        for (i in 1..THREADS) {
            allList.add(TestData("asdf", i))
        }

        cache.cache.forEach {
            assertTrue(allList.contains(it.entry))
        }
    }

    @Test
    fun threadLocalClearFailsInUse() {
        val cache = ThreadLocalCache { TestData("asdf", 1) }
        cache.mineOrAlign()
        assertFails { cache.clear() }
    }

    @Test
    fun threadLocalClearBlock() {
        val cache = ThreadLocalCache { TestData("asdf", 1) }
        cache.mineOrAlign()
        cache.mineRelease()
        cache.clear {
            assertEquals(it, TestData("asdf", 1))
        }
    }

    @Test
    fun threadLocalCacheRefIsLocal() {
        val ops = ThreadOperations { ThreadLocalCache { TestData("asdf", 1) } }
        val THREADS = 5
        for (i in 0 until THREADS) {
            ops.exe {
                assertNull(it.mineOrNone())
                it.mineOrAlign()
                assertNotNull(it.mineOrNone())
                sleep(1000)
                it.mineRelease()
                assertNull(it.mineOrNone())
            }
        }

        val cache = ops.run(THREADS)
        assertEquals(THREADS, cache.cache.size)
        cache.cache.forEach {
            assertEquals(it.entry, TestData("asdf", 1))
        }

        assertNull(cache.mineOrNone())
    }

    @Test
    fun mutatorStatementCacheStrategy() {
        val ops = ThreadOperations { MutatorStatements() }
        val THREADS = 4
        for (i in 0 until THREADS) {
            ops.exe {
                val inst = it.myStatementInstance()
                inst.bindLong(1, i.toLong())
                sleep(1000)
                val stmt = MockStatement()
                inst.binds.forEach {
                    it.value(stmt)
                }
                assertEquals(i.toLong(), stmt.boundL)
                it.releaseInstance()
            }
        }

        val statements = ops.run(THREADS)
        assertEquals(statements.statementCache.cache.size, THREADS)
    }

    @Test
    fun bindingAccumulator() {
        val ac = BindingAccumulator()
        ac.bindLong(1, 1L)
        assertEquals(1, ac.binds.size)
        ac.bindDouble(1, 1.0)
        assertEquals(1, ac.binds.size)

        val bytes = ByteArray(3) { 97.toByte() }//'a'
        ac.bindLong(1, 1L)
        ac.bindDouble(2, 1.0)
        assertEquals(2, ac.binds.size)
        ac.bindString(3, "asdf")
        ac.bindBytes(4, bytes)
        assertEquals(4, ac.binds.size)

        val stmt = MockStatement()
        ac.binds.forEach {
            it.value(stmt)
        }

        assertEquals(1L, stmt.boundL)
        assertEquals(1.0, stmt.boundD)
        assertEquals("asdf", stmt.boundS)
        assertEquals(3, stmt.boundB.size)
    }

    @Test
    fun multipleThreadsTransactions() {
        val ops = ThreadOperations { database.getConnection() }

        val THREADS = 4
        val LOOPS = 1000
        val stmt = database.getConnection().prepareStatement("insert into test(id, value)values(?, ?)", INSERT, 2)
        val transacter = object : Transacter(database) {}
        transacter.freeze()

        for (i in 0 until THREADS) {
            ops.exe {
                transacter.transaction {
                    for (j in 0 until LOOPS) {
                        val idInt = i * LOOPS + j
                        stmt.bindLong(1, idInt.toLong())
                        stmt.bindString(2, "row $idInt")
                        stmt.execute()
                    }

                    sleep(1200)
                }
            }
        }

        ops.run(THREADS)

        val sqliterSqlDatabase = database as SqliterSqlDatabase
        assertEquals(THREADS, sqliterSqlDatabase.connectionCache.cache.size)

        val query = database.getConnection().prepareStatement("select count(*) from test",
                SELECT, 0).executeQuery()
        query.next()
        val count = query.getLong(0)
        query.close()
        assertEquals((THREADS * LOOPS).toLong(), count)

    }

    @Test
    fun preparedStatementQueryCache() {
        val sqliterSqlDatabase = database as SqliterSqlDatabase
        val conn = database.getConnection()
        val stmt = conn.prepareStatement("select * from test", SELECT, 0)
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)
        val query = stmt.executeQuery()
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(1, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)
        query.close()
        assertEquals(1, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)

        val queryA = stmt.executeQuery()
        val queryB = stmt.executeQuery()
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(2, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)
        queryA.close()
        queryB.close()
        assertEquals(1, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)

        val ops = ThreadOperations { stmt }
        val THREAD = 4
        val collectCursors = frozenLinkedList<SqlCursor>()
        for (i in 0 until THREAD) {
            ops.exe {
                collectCursors.add(stmt.executeQuery())
            }
        }

        ops.run(THREAD)

        assertEquals(0, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(THREAD, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)
        collectCursors.forEach { it.close() }
        assertEquals(1, sqliterSqlDatabase.singleOpConnection.statementCache.size)
        assertEquals(0, sqliterSqlDatabase.singleOpConnection.cursorCollection.size)
    }

    internal class MockDbContext() : RealDatabaseContext {
        override fun <R> accessConnection(block: ThreadConnection.() -> R): R {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


    class MockStatement() : Statement {
        var boundL = 0L
        var boundD = 0.toDouble()
        var boundS = ""
        var boundB = ByteArray(0)

        override fun bindBlob(index: Int, value: ByteArray) {
            boundB = value
        }

        override fun bindDouble(index: Int, value: Double) {
            boundD = value
        }

        override fun bindLong(index: Int, value: Long) {
            boundL = value
        }

        override fun bindNull(index: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun bindParameterIndex(paramName: String): Int {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun bindString(index: Int, value: String) {
            boundS = value
        }

        override fun clearBindings() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun execute() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun executeInsert(): Long {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun executeUpdateDelete(): Int {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun finalizeStatement() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun query(): Cursor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun resetStatement() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    data class TestData(val s: String, val count: Int)

}