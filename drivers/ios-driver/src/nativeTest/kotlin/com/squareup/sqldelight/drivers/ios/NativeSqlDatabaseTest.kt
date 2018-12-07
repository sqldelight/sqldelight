package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.*
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.freeze
import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.currentTimeMillis
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.test.*

class NativeSqlDatabaseTest:LazyDbBaseTest(){

    @Test
    fun `close with open transaction fails`(){
        transacter.transaction {
            assertFails { database.close() }
        }

        //Still working? There's probably a better general test for this.
        val stmt = database.getConnection().prepareStatement("select * from test", SqlPreparedStatement.Type.SELECT, 0)
        val query = stmt.executeQuery()
        query.next()
        query.close()
    }

    @Test
    fun `wrapConnection does not close connection`(){
        val closed = AtomicBoolean(true)
        val config = DatabaseConfiguration(
                name = "testdb",
                version = 1,
                inMemory = true,
                create = { connection ->
                    wrapConnection(connection) {
                        defaultSchema().create(it)
                    }

                    closed.value = connection.closed
                })
        val dbm = createDatabaseManager(config)
        dbm.createMultiThreadedConnection().close()

        assertFalse(closed.value)
    }

    //Kind of a sanity check
    @Test
    fun `threads share statement main connection multithreaded`(){
        altInit(defaultConfiguration(defaultSchema()).copy(inMemory = true))
        val conn = database.getConnection()
        val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)", SqlPreparedStatement.Type.INSERT, 2)

        val ops = ThreadOperations {stmt}
        val INSERTS = 10_000
        for(i in 0 until INSERTS){
            ops.exe {
                stmt.bindLong(1, i.toLong())
                stmt.bindString(2, "Hey $i")
                stmt.execute()
            }
        }

        ops.run(10)

        assertEquals(INSERTS.toLong(), countTestRows(conn))
        val strSet = mutableSetOf<String>()
        val query = conn.prepareStatement("select id, value from test", SqlPreparedStatement.Type.SELECT, 0).executeQuery()
        var sum = 0L
        while (query.next()){
            strSet.add(query.getString(1)!!)
            sum += query.getLong(0)!!
        }

        assertEquals(sum, (0 until INSERTS).fold(0L){a, b -> a+b})
        assertEquals(INSERTS, strSet.size)
    }

    @Test
    fun `failing transaction clears lock`(){
        val conn = database.getConnection()
        val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)", SqlPreparedStatement.Type.INSERT, 2)

        transacter.transaction {
            stmt.bindLong(1, 1)
            stmt.bindString(2, "asdf")
            stmt.execute()
            throw IllegalStateException("Fail")
        }

        transacter.transaction {
            try {
                stmt.bindLong(1, 1)
                stmt.bindString(2, "asdf")
                stmt.execute()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        assertEquals(1, countTestRows(conn))
    }

    @Test
    fun `bad bind doens't taint future binding`(){
        val conn = database.getConnection()
        val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)", SqlPreparedStatement.Type.INSERT, 2)

        transacter.transaction {
            stmt.bindLong(1, 1)
            stmt.bindString(3, "asdf")
            stmt.execute()
        }

        transacter.transaction {
            stmt.bindLong(1, 1)
            stmt.bindString(2, "asdf")
            stmt.execute()
        }

        assertEquals(1, countTestRows(conn))
    }

    //Can't run this till https://github.com/touchlab/SQLiter/issues/8
    /*@Test
    fun `leaked resource fails close`(){
        val sqliterdb = database as NativeSqlDatabase
        val leakedStatement = sqliterdb.singleOpConnection.connection.createStatement("select * from test")
        assertFails { database.close() }
        assertFails { leakedStatement.finalizeStatement() }

        //TODO. Should research on exactly why re-closing doesn't fail. SQLiter will need an update as it zeros out
        //pointer even when close fails. However, for our purposes (ensuring first close attempt bombs), we're OK
        database.close()
    }*/

    @Test
    fun `failures don't leak resources`(){
        val conn = database.getConnection()
        val transacter = transacter
        val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)", SqlPreparedStatement.Type.INSERT, 2)

        val ops = ThreadOperations {stmt}
        val THREADS = 3
        for (i in 1..10) {
            ops.exe {
                exeQuiet{transacter.transaction {

                    for (i in 0 until 10){
                        stmt.bindLong(1, i.toLong())
                        stmt.bindString(2, "Hey $i")
                        stmt.execute()
                    }

                    throw IllegalStateException("Nah")
                }}

                exeQuiet {
                    stmt.bindLong(1, i.toLong())
                    stmt.bindString(3, "Hey $i")
                    stmt.execute()
                }

                val query = conn.prepareStatement("select id, value from test", SqlPreparedStatement.Type.SELECT, 0).executeQuery()
                query.next()

                exeQuiet {
                    val query = conn.prepareStatement("select id, value from toast", SqlPreparedStatement.Type.SELECT, 0).executeQuery()
                    query.next()
                }
            }
        }


        ops.run(THREADS)

        val literdb = database as NativeSqlDatabase
        assertEquals(1, literdb.connectionCache.cache.size)
        assertEquals(10, literdb.singleOpConnection.cursorCollection.size)
        assertEquals(0, countTestRows(conn))

        //If we've leaked anything the test cleanup will fail...
    }

    fun exeQuiet(proc:()->Unit) {
        try {
            proc()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `MutatorStatements cache strategy has separate instances per-thread`() {
        val ops = ThreadOperations { MutatorStatements() }
        val THREADS = 4
        val waiter = WaitThreads(THREADS, 5000)

        for (i in 0 until THREADS) {
            ops.exe {

                val inst = waiter.wait {
                    val inst = it.myStatementInstance()
                    inst.bindLong(1, i.toLong())
                    inst
                }

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
    fun `BindingAccumulator basic test`() {
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
    fun `multiple thread transactions share single db connection`() {
        val THREADS = 4
        val LOOPS = 1000
        val stmt = database.getConnection().prepareStatement("insert into test(id, value)values(?, ?)", SqlPreparedStatement.Type.INSERT, 2)

        insertThreadLoop(0, THREADS, transacter, LOOPS, stmt)
        insertThreadLoop(THREADS * LOOPS, THREADS, transacter, LOOPS, stmt)

        assertEquals((THREADS * LOOPS * 2).toLong(), countTestRows(database.getConnection()))

        val sqliterSqlDatabase = database as NativeSqlDatabase

        //Ran loop twice. Reuse cached connections.
        assertEquals(1, sqliterSqlDatabase.connectionCache.cache.size)
    }

    private fun countTestRows(conn: SqlDatabaseConnection): Long {
        val query = conn.prepareStatement("select count(*) from test",
                SqlPreparedStatement.Type.SELECT, 0).executeQuery()
        query.next()
        val count = query.getLong(0)
        query.close()
        return count!!
    }

    private fun insertThreadLoop(start:Int, THREADS: Int, transacter: Transacter, LOOPS: Int, stmt: SqlPreparedStatement) {
        val ops = ThreadOperations { database.getConnection() }

        for (i in 0 until THREADS) {
            ops.exe {
                transacter.transaction {
                    try {//Make sure other transactions start before we finish
                        for (j in 0 until LOOPS) {
                            val idInt = i * LOOPS + j + start
                            stmt.bindLong(1, idInt.toLong())
                            stmt.bindString(2, "row $idInt")
                            stmt.execute()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }
        }

        ops.run(THREADS)
    }

    @Test
    fun `query statements cached but only 1`() {
        val sqliterSqlDatabase = database as NativeSqlDatabase
        val conn = database.getConnection()
        val stmt = conn.prepareStatement("select * from test", SqlPreparedStatement.Type.SELECT, 0)
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

    @Test
    fun `query exception clears statement`(){
        val sqliterSqlDatabase = database as NativeSqlDatabase
        val conn = database.getConnection()
        val stmt = conn.prepareStatement("select * from test", SqlPreparedStatement.Type.SELECT, 0)
        stmt.bindLong(1, 2L)

        assertFails { stmt.executeQuery() }

        assertEquals(0, sqliterSqlDatabase.singleOpConnection.statementCache.size)
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
}

class ThreadLocalCacheTest{
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
        val waiter = WaitThreads(THREADS, 5000)
        for (i in 0 until THREADS) {
            ops.exe {
                waiter.wait {
                    assertNull(it.mineOrNone())
                    it.mineOrAlign()
                    assertNotNull(it.mineOrNone())
                }
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
}

data class TestData(val s: String, val count: Int)

/**
 * Busy loops thread till all are done or timeout. Would be better to suspend, but
 * this is simpler for now.
 */
class WaitThreads(private val threadCount: Int, private val timeout:Long, private val exBlock:(Throwable)->Unit = {it.printStackTrace()}){
    private val finishedCount = AtomicInt(0)

    init {
        freeze()
    }

    fun <R> wait(block:()->R):R{
        val result = try {
            block()
        } catch (t:Throwable){
            exBlock(t)
            throw t
        } finally {
            finishedCount.incrementAndGet()
        }

        val start = currentTimeMillis()
        val waitTill = start + timeout

        while (threadCount > finishedCount.value){
            if(currentTimeMillis() >= waitTill){
                throw IllegalStateException("Thread wait timeout")
            }
            sleep(100)
        }

        return result
    }
}