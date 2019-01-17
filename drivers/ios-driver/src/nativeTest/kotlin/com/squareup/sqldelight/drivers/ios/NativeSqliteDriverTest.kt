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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame

//Run tests with WAL db
class NativeSqliteDriverTestWAL : NativeSqliteDriverTest() {
  override val memory: Boolean = false
}

//Run tests with memory db
class NativeSqliteDriverTestMemory : NativeSqliteDriverTest() {
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

abstract class NativeSqliteDriverTest : LazyDriverBaseTest() {

  @Test
  fun `close with open transaction fails`(){
      transacter.transaction {
          assertFails { driver.close() }
      }

      //Still working? There's probably a better general test for this.
      val stmt = driver.getConnection().prepareStatement("select * from test", SqlPreparedStatement.Type.SELECT, 0)
      val query = stmt.executeQuery()
      query.next()
      query.close()
  }

  //Kind of a sanity check
  @Test
  fun `threads share statement main connection multithreaded`() {
    altInit(defaultConfiguration(defaultSchema()).copy(inMemory = true))
    val ops = ThreadOperations { }
    val INSERTS = 10_000
    for (i in 0 until INSERTS) {
      ops.exe {
        driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
          bindLong(1, i.toLong())
          bindString(2, "Hey $i")
        }
      }
    }

    ops.run(10)

    assertEquals(INSERTS.toLong(), countTestRows(driver))
    val strSet = mutableSetOf<String>()
    val query = driver.executeQuery(2, "select id, value from test", 0)
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
      driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
        bindLong(1, 1)
        bindString(2, "asdf")
      }

      throw IllegalStateException("Fail")
    }

    transacter.transaction {
      try {
        driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
          bindLong(1, 1)
          bindString(2, "asdf")
        }

      } catch (e: Exception) {
        e.printStackTrace()
        throw e
      }
    }

    assertEquals(1, countTestRows(driver))
  }

  @Test
  fun `bad bind doens't taint future binding`() {
    transacter.transaction {
      driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
        bindLong(1, 1)
        bindString(3, "asdf")
      }
    }

    transacter.transaction {
      driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
        bindLong(1, 1)
        bindString(2, "asdf")
      }
    }

    assertEquals(1, countTestRows(driver))
  }

  @Test
  fun `failed bind dumps sqlite statement`() {
    assertFails {
      driver.execute(1, "insert into test(id, value) values(?, ?)", 2) {
        assertEquals(0, driver.queryPool.entry.statementCache.size)
        bindLong(1, 1L)
        throw assertFails { bindLong(3, 1L) }
      }
    }
    assertEquals(1, driver.queryPool.entry.statementCache.size)
  }

  @Test
  fun `failures don't leak resources`() {
    val transacter = transacter

    val ops = ThreadOperations { }
    val threads = 3
    for (i in 1..10) {
      ops.exe {
        exeQuiet {
          transacter.transaction {

            for (i in 0 until 10) {
              driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
                bindLong(1, i.toLong())
                bindString(2, "Hey $i")
              }
            }

            throw IllegalStateException("Nah")
          }
        }

        exeQuiet {
          driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
            bindLong(1, i.toLong())
            bindString(3, "Hey $i")
          }
        }

        driver.executeQuery(2, "select id, value from test", 0).next()

        exeQuiet {
          driver.executeQuery(3, "select id, value from toast", 0).next()
        }
      }
    }

    ops.run(threads)

    assertEquals(10, driver.queryPool.entry.cursorCollection.size)
    assertEquals(0, countTestRows(driver))

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

    assertEquals((THREADS * LOOPS * GLOBALLOOPS).toLong(), countTestRows(driver))
  }

  private fun countTestRows(conn: NativeSqliteDriver): Long {
    val query = conn.executeQuery(10, "select count(*) from test", 0)
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
    val ops = ThreadOperations { driver }

    for (i in 0 until THREADS) {
      ops.exe { conn ->

        transacter.transaction {
          try {
            for (j in 0 until LOOPS) {
              conn.execute(1, "insert into test(id, value)values(?, ?)", 2) {
                val idInt = i * LOOPS + j + start
                bindLong(1, idInt.toLong())
                bindString(2, "row $idInt")
              }
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
    val stmt = { driver.executeQuery(1, "select * from test", 0) }

    assertEquals(0, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.queryPool.entry.cursorCollection.size)

    val query = stmt()

    assertEquals(0, driver.queryPool.entry.statementCache.size)
    assertEquals(1, driver.queryPool.entry.cursorCollection.size)
    query.close()

    assertEquals(1, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.queryPool.entry.cursorCollection.size)

    val queryA = stmt()
    val queryB = stmt()

    assertEquals(0, driver.queryPool.entry.statementCache.size)
    assertEquals(2, driver.queryPool.entry.cursorCollection.size)

    queryA.close()
    queryB.close()

    assertEquals(1, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.queryPool.entry.cursorCollection.size)

    val ops = ThreadOperations { stmt }
    val THREAD = 4
    val collectCursors = frozenLinkedList<SqlCursor>()
    for (i in 0 until THREAD) {
      ops.exe {
        collectCursors.add(stmt())
      }
    }

    ops.run(THREAD)

    assertEquals(0, driver.queryPool.entry.statementCache.size)
    assertEquals(THREAD, driver.queryPool.entry.cursorCollection.size)
    collectCursors.forEach { it.close() }
    assertEquals(1, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.queryPool.entry.cursorCollection.size)
  }

  @Test
  fun `query exception clears statement`() {
    assertFails {
      driver.executeQuery(1, "select * from test", 0) {
        throw assertFails { bindLong(1, 2L) }
      }
    }

    assertEquals(1, driver.queryPool.entry.statementCache.size)
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
    driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
      bindLong(1, 22L)
      bindString(2, "Hey 22")
    }

    assertEquals(1, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.transactionPool.entry.statementCache.size)

    transacter.transaction {
      driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
        bindLong(1, 33L)
        bindString(2, "Hey 33")
      }
    }

    assertEquals(1, driver.queryPool.entry.statementCache.size)
    assertEquals(1, driver.transactionPool.entry.statementCache.size)

    val statement =
        driver.transactionPool.entry.statementCache.entries.iterator().next().value

    transacter.transaction {
      driver.execute(1, "insert into test(id, value)values(?, ?)", 2) {
        bindLong(1, 34L)
        bindString(2, "Hey 34")
      }
    }

    assertEquals(1, driver.queryPool.entry.statementCache.size)
    assertEquals(1, driver.transactionPool.entry.statementCache.size)

    assertSame(
        driver.transactionPool.entry.statementCache.entries.iterator().next().value,
        statement)
  }

  @Test
  fun `null identifier doesn't cache`() {
    val transacter = transacter
    driver.execute(null, "insert into test(id, value)values(?, ?)", 2) {
      bindLong(1, 22L)
      bindString(2, "Hey 22")
    }


    assertEquals(0, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.transactionPool.entry.statementCache.size)

    transacter.transaction {
      driver.execute(null, "insert into test(id, value)values(?, ?)", 2) {
        bindLong(1, 22L)
        bindString(2, "Hey 22")
      }
    }

    assertEquals(0, driver.queryPool.entry.statementCache.size)
    assertEquals(0, driver.transactionPool.entry.statementCache.size)
  }

}