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

//Run tests with WAL db
class NativeSqlDatabaseTestWAL : NativeSqlDatabaseTest() {
  override val memory: Boolean = false
}

//Run tests with memory db
class NativeSqlDatabaseTestMemory : NativeSqlDatabaseTest() {
  override val memory: Boolean = true
}

abstract class NativeSqlDatabaseTest : LazyDbBaseTest() {

  //Can't run this till https://github.com/touchlab/SQLiter/issues/8
  /*@Test
  fun `close with open transaction fails`(){
      transacter.transaction {
          assertFails { database.close() }
      }

      //Still working? There's probably a better general test for this.
      val stmt = database.getConnection().prepareStatement("select * from test", SqlPreparedStatement.Type.SELECT, 0)
      val query = stmt.executeQuery()
      query.next()
      query.close()
  }*/

  @Test
  fun `wrapConnection does not close connection`() {
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
  fun `threads share statement main connection multithreaded`() {
    altInit(defaultConfiguration(defaultSchema()).copy(inMemory = true))
    val conn = database.getConnection()
    val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

    val ops = ThreadOperations { stmt }
    val INSERTS = 10_000
    for (i in 0 until INSERTS) {
      ops.exe {
        stmt.bindLong(1, i.toLong())
        stmt.bindString(2, "Hey $i")
        stmt.execute()
      }
    }

    ops.run(10)

    assertEquals(INSERTS.toLong(), countTestRows(conn))
    val strSet = mutableSetOf<String>()
    val query =
        conn.prepareStatement("select id, value from test", SqlPreparedStatement.Type.SELECT, 0)
            .executeQuery()
    var sum = 0L
    while (query.next()) {
      strSet.add(query.getString(1)!!)
      sum += query.getLong(0)!!
    }

    assertEquals(sum, (0 until INSERTS).fold(0L) { a, b -> a + b })
    assertEquals(INSERTS, strSet.size)
  }

  @Test
  fun `failing transaction clears lock`() {
    val conn = database.getConnection()
    val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

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
  fun `bad bind doens't taint future binding`() {
    val conn = database.getConnection()
    val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

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
  fun `failed bind dumps sqlite statement`() {
    val conn = database.getConnection()
    val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2) as MutatorPreparedStatement

    assertNull(stmt.dbStatement.value)
    stmt.bindLong(1, 1L)
    assertNotNull(stmt.dbStatement.value)
    assertFails { stmt.bindLong(3, 1L) }
    assertNull(stmt.dbStatement.value)
  }

  @Test
  fun `failures don't leak resources`() {
    val conn = database.getConnection()
    val transacter = transacter
    val stmt = conn.prepareStatement("insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

    val ops = ThreadOperations { stmt }
    val THREADS = 3
    for (i in 1..10) {
      ops.exe {
        exeQuiet {
          transacter.transaction {

            for (i in 0 until 10) {
              stmt.bindLong(1, i.toLong())
              stmt.bindString(2, "Hey $i")
              stmt.execute()
            }

            throw IllegalStateException("Nah")
          }
        }

        exeQuiet {
          stmt.bindLong(1, i.toLong())
          stmt.bindString(3, "Hey $i")
          stmt.execute()
        }

        val query =
            conn.prepareStatement("select id, value from test", SqlPreparedStatement.Type.SELECT, 0)
                .executeQuery()
        query.next()

        exeQuiet {
          val query =
              conn.prepareStatement("select id, value from toast", SqlPreparedStatement.Type.SELECT,
                  0).executeQuery()
          query.next()
        }
      }
    }

    ops.run(THREADS)

    val literdb = database as NativeSqlDatabase
    assertEquals(10, literdb.queryPool.entry.cursorCollection.size)
    assertEquals(0, countTestRows(conn))

    //If we've leaked anything the test cleanup will fail...
  }

  fun exeQuiet(proc: () -> Unit) {
    try {
      proc()
    } catch (e: Exception) {
    }
  }

  @Test
  fun `multiple thread transactions wait and complete successfully`() {
    val THREADS = 25
    val LOOPS = 50
    val stmt = database.getConnection().prepareStatement("insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

    val GLOBALLOOPS = 100

    for (i in 0 until GLOBALLOOPS) {
      insertThreadLoop(THREADS * LOOPS * i, THREADS, transacter, LOOPS, stmt)
    }

    assertEquals((THREADS * LOOPS * GLOBALLOOPS).toLong(), countTestRows(database.getConnection()))
  }

  private fun countTestRows(conn: SqlDatabaseConnection): Long {
    val query = conn.prepareStatement("select count(*) from test",
        SqlPreparedStatement.Type.SELECT, 0).executeQuery()
    query.next()
    val count = query.getLong(0)
    query.close()
    return count!!
  }

  private fun insertThreadLoop(
    start: Int,
    THREADS: Int,
    transacter: Transacter,
    LOOPS: Int,
    stmt: SqlPreparedStatement
  ) {
    val ops = ThreadOperations { database.getConnection() }

    for (i in 0 until THREADS) {
      ops.exe {
        transacter.transaction {
          try {
            for (j in 0 until LOOPS) {
              val idInt = i * LOOPS + j + start
              stmt.bindLong(1, idInt.toLong())
              stmt.bindString(2, "row $idInt")
              stmt.execute()
            }
          } catch (e: Throwable) {
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
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)
    val query = stmt.executeQuery()
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(1, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)
    query.close()
    assertEquals(1, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)

    val queryA = stmt.executeQuery()
    val queryB = stmt.executeQuery()
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(2, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)
    queryA.close()
    queryB.close()
    assertEquals(1, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)

    val ops = ThreadOperations { stmt }
    val THREAD = 4
    val collectCursors = frozenLinkedList<SqlCursor>()
    for (i in 0 until THREAD) {
      ops.exe {
        collectCursors.add(stmt.executeQuery())
      }
    }

    ops.run(THREAD)

    assertEquals(0, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(THREAD, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)
    collectCursors.forEach { it.close() }
    assertEquals(1, sqliterSqlDatabase.queryPool.entry.statementCache.size)
    assertEquals(0, sqliterSqlDatabase.queryPool.entry.cursorCollection.size)
  }

  @Test
  fun `query exception clears statement`() {
    val sqliterSqlDatabase = database as NativeSqlDatabase
    val conn = database.getConnection()
    val stmt = conn.prepareStatement("select * from test", SqlPreparedStatement.Type.SELECT, 0)
    stmt.bindLong(1, 2L)

    assertFails { stmt.executeQuery() }

    assertEquals(0, sqliterSqlDatabase.queryPool.entry.statementCache.size)
  }

  @Test
  fun `SinglePool access locked`() {
    val ops = ThreadOperations { SinglePool { AtomicInt(0) } }
    val failed = AtomicBoolean(false)
    for (i in 0 until 150) {
      ops.exe {
        it.access {
          val atStart = it.incrementAndGet()
          if (atStart != 1)
            failed.value = true

          sleep(10)

          val atEnd = it.decrementAndGet()
          if (atEnd != 0)
            failed.value = true
        }
      }
    }

    ops.run(5)
    assertFalse(failed.value)
  }

  @Test
  fun `SinglePool re-borrow fails`() {
    val pool = SinglePool<Unit>({})
    val borrowed = pool.borrowEntry()
    assertFails { pool.borrowEntry() }
    borrowed.release()
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
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindParameterIndex(paramName: String): Int {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindString(index: Int, value: String) {
      boundS = value
    }

    override fun clearBindings() {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeInsert(): Long {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeUpdateDelete(): Int {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun finalizeStatement() {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun query(): Cursor {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resetStatement() {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

  }
}
