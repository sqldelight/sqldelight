package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.stately.collections.frozenLinkedList
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.testhelp.concurrency.ThreadOperations
import co.touchlab.testhelp.concurrency.sleep
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertSame

//Run tests with WAL db
class NativeSqlDatabaseTestWAL : NativeSqlDatabaseTest() {
  override val memory: Boolean = false
}

//Run tests with memory db
class NativeSqlDatabaseTestMemory : NativeSqlDatabaseTest() {
  override val memory: Boolean = true

  @Test
  fun `wrapConnection does not close connection`() {
    val closed = AtomicBoolean(true)
    val config = DatabaseConfiguration(
        name = "memorydb",
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

  //Kind of a sanity check
  @Test
  fun `threads share statement main connection multithreaded`() {
    altInit(defaultConfiguration(defaultSchema()).copy(inMemory = true))
    val ops = ThreadOperations { }
    val INSERTS = 10_000
    for (i in 0 until INSERTS) {
      ops.exe {
        val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
            SqlPreparedStatement.Type.INSERT, 2)

        stmt.bindLong(1, i.toLong())
        stmt.bindString(2, "Hey $i")
        stmt.execute()
      }
    }

    ops.run(10)

    assertEquals(INSERTS.toLong(), countTestRows(database))
    val strSet = mutableSetOf<String>()
    val query =
        database.prepareStatement(2, "select id, value from test", SqlPreparedStatement.Type.SELECT, 0)
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
    transacter.transaction {
      val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
          SqlPreparedStatement.Type.INSERT, 2)

      stmt.bindLong(1, 1)
      stmt.bindString(2, "asdf")
      stmt.execute()
      throw IllegalStateException("Fail")
    }

    transacter.transaction {
      try {
        val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
            SqlPreparedStatement.Type.INSERT, 2)

        stmt.bindLong(1, 1)
        stmt.bindString(2, "asdf")
        stmt.execute()
      } catch (e: Exception) {
        e.printStackTrace()
        throw e
      }
    }

    assertEquals(1, countTestRows(database))
  }

  @Test
  fun `bad bind doens't taint future binding`() {
    transacter.transaction {
      val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
          SqlPreparedStatement.Type.INSERT, 2)
      stmt.bindLong(1, 1)
      stmt.bindString(3, "asdf")
      stmt.execute()
    }

    transacter.transaction {
      val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
          SqlPreparedStatement.Type.INSERT, 2)
      stmt.bindLong(1, 1)
      stmt.bindString(2, "asdf")
      stmt.execute()
    }

    assertEquals(1, countTestRows(database))
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
    val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2) as SqliterStatement

    assertEquals(0, database.queryPool.entry.statementCache.size)
    stmt.bindLong(1, 1L)
    assertFails { stmt.bindLong(3, 1L) }
    assertEquals(1, database.queryPool.entry.statementCache.size)
  }

  @Test
  fun `failures don't leak resources`() {
    val transacter = transacter

    val ops = ThreadOperations { }
    val THREADS = 3
    for (i in 1..10) {
      ops.exe {
        exeQuiet {
          transacter.transaction {

            for (i in 0 until 10) {
              val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
                  SqlPreparedStatement.Type.INSERT, 2)

              stmt.bindLong(1, i.toLong())
              stmt.bindString(2, "Hey $i")
              stmt.execute()
            }

            throw IllegalStateException("Nah")
          }
        }

        exeQuiet {
          val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
              SqlPreparedStatement.Type.INSERT, 2)

          stmt.bindLong(1, i.toLong())
          stmt.bindString(3, "Hey $i")
          stmt.execute()
        }

        val query =
            database.prepareStatement(2, "select id, value from test", SqlPreparedStatement.Type.SELECT,
                0)
                .executeQuery()
        query.next()

        exeQuiet {
          val prepareStatement = database.prepareStatement(3, "select id, value from toast",
              SqlPreparedStatement.Type.SELECT,
              0)
          val query =
              prepareStatement.executeQuery()
          query.next()
        }
      }
    }

    ops.run(THREADS)

    assertEquals(10, database.queryPool.entry.cursorCollection.size)
    assertEquals(0, countTestRows(database))

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

    val GLOBALLOOPS = 100

    for (i in 0 until GLOBALLOOPS) {
      insertThreadLoop(THREADS * LOOPS * i, THREADS, transacter, LOOPS)
    }

    assertEquals((THREADS * LOOPS * GLOBALLOOPS).toLong(), countTestRows(database))
  }

  private fun countTestRows(conn: NativeSqlDatabase): Long {
    val query = conn.prepareStatement(10, "select count(*) from test",
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
    LOOPS: Int
  ) {
    val ops = ThreadOperations { database }

    for (i in 0 until THREADS) {
      ops.exe { conn ->

        transacter.transaction {
          try {
            for (j in 0 until LOOPS) {
              val stmt = conn.prepareStatement(1, "insert into test(id, value)values(?, ?)",
                  SqlPreparedStatement.Type.INSERT, 2)
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
    val stmt =
        { database.prepareStatement(1, "select * from test", SqlPreparedStatement.Type.SELECT, 0) }

    assertEquals(0, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.queryPool.entry.cursorCollection.size)

    val query = stmt().executeQuery()

    assertEquals(0, database.queryPool.entry.statementCache.size)
    assertEquals(1, database.queryPool.entry.cursorCollection.size)
    query.close()

    assertEquals(1, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.queryPool.entry.cursorCollection.size)

    val queryA = stmt().executeQuery()
    val queryB = stmt().executeQuery()

    assertEquals(0, database.queryPool.entry.statementCache.size)
    assertEquals(2, database.queryPool.entry.cursorCollection.size)

    queryA.close()
    queryB.close()

    assertEquals(1, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.queryPool.entry.cursorCollection.size)

    val ops = ThreadOperations { stmt }
    val THREAD = 4
    val collectCursors = frozenLinkedList<SqlCursor>()
    for (i in 0 until THREAD) {
      ops.exe {
        collectCursors.add(stmt().executeQuery())
      }
    }

    ops.run(THREAD)

    assertEquals(0, database.queryPool.entry.statementCache.size)
    assertEquals(THREAD, database.queryPool.entry.cursorCollection.size)
    collectCursors.forEach { it.close() }
    assertEquals(1, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.queryPool.entry.cursorCollection.size)
  }

  @Test
  fun `query exception clears statement`() {
    val stmt = database.prepareStatement(1, "select * from test", SqlPreparedStatement.Type.SELECT, 0)
    assertFails { stmt.bindLong(1, 2L) }

    assertEquals(1, database.queryPool.entry.statementCache.size)
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
    val pool = SinglePool {}
    val borrowed = pool.borrowEntry()
    assertFails { pool.borrowEntry() }
    borrowed.release()
  }

  @Test
  fun `caching by index works as expected`() {
    val transacter = transacter
    val stmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

    stmt.bindLong(1, 22L)
    stmt.bindString(2, "Hey 22")
    stmt.execute()

    assertEquals(1, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.transactionPool.entry.statementCache.size)

    transacter.transaction {
      val transStmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
          SqlPreparedStatement.Type.INSERT, 2)
      transStmt.bindLong(1, 33L)
      transStmt.bindString(2, "Hey 33")
      transStmt.execute()
    }

    assertEquals(1, database.queryPool.entry.statementCache.size)
    assertEquals(1, database.transactionPool.entry.statementCache.size)

    val statement =
        database.transactionPool.entry.statementCache.entries.iterator().next().value

    transacter.transaction {
      val transStmt = database.prepareStatement(1, "insert into test(id, value)values(?, ?)",
          SqlPreparedStatement.Type.INSERT, 2)

      transStmt.bindLong(1, 34L)
      transStmt.bindString(2, "Hey 34")
      transStmt.execute()
    }

    assertEquals(1, database.queryPool.entry.statementCache.size)
    assertEquals(1, database.transactionPool.entry.statementCache.size)

    assertSame(
        database.transactionPool.entry.statementCache.entries.iterator().next().value,
        statement)
  }

  @Test
  fun `null identifier doesn't cache`() {
    val transacter = transacter
    val stmt = database.prepareStatement(null, "insert into test(id, value)values(?, ?)",
        SqlPreparedStatement.Type.INSERT, 2)

    stmt.bindLong(1, 22L)
    stmt.bindString(2, "Hey 22")
    stmt.execute()

    assertEquals(0, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.transactionPool.entry.statementCache.size)

    transacter.transaction {
      stmt.bindLong(1, 33L)
      stmt.bindString(2, "Hey 33")
      stmt.execute()
    }

    assertEquals(0, database.queryPool.entry.statementCache.size)
    assertEquals(0, database.transactionPool.entry.statementCache.size)
  }

}