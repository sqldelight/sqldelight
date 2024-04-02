import com.example.db.DbHelper
import com.example.db.genLong
import com.example.db.genString
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Test
import tables.TableA
import tables.TableB
import tables.TableC

/**
 * I run two threads. Each thread selects all records from TableA, TableB and TableC
 *
 * Sometimes error is: java.sql.SQLException: database in auto-commit mode
 * And sometimes is: org.sqlite.SQLiteException: \[SQLITE_ERROR\] SQL error or missing database (cannot commit - no transaction is active)
 */
class MultithreadedTest {
  @After fun after() {
    File("test.db").delete()
  }

  @Test fun withTransactions() {
    val dbHelper = DbHelper("WAL", true)

    try {
      insertSomeData(dbHelper)
      selectsThatFail(dbHelper)
    } finally {
      dbHelper.close()
    }
  }

  @Test fun withoutTransactions() {
    val dbHelper = DbHelper("WAL", true)

    try {
      insertSomeData(dbHelper)
      selectsThatWork(dbHelper)
    } finally {
      dbHelper.close()
    }
  }
}

fun selectsThatFail(dbHelper: DbHelper) {
  val runs = AtomicInteger(0)
  for (x in 0..1) {
    thread(true) {
      try {
        selectWithTransactions(dbHelper)
      } finally {
        runs.incrementAndGet()
      }
    }
  }
  while (runs.get() != 2) Thread.yield()
}

fun selectsThatWork(dbHelper: DbHelper) {
  val runs = AtomicInteger(0)
  for (x in 0..1) {
    thread(true) {
      try {
        selectWithoutTransactions(dbHelper)
      } finally {
        runs.incrementAndGet()
      }
    }
  }
  while (runs.get() != 2) Thread.yield()
}

private fun selectWithTransactions(dbHelper: DbHelper) {
  dbHelper.database.transaction {
    val aList = dbHelper.database.tableAQueries.selectAll().executeAsList()
  }

  dbHelper.database.transaction {
    val bList = dbHelper.database.tableBQueries.selectAll().executeAsList()
  }

  dbHelper.database.transaction {
    val cList = dbHelper.database.tableCQueries.selectAll().executeAsList()
  }
}

private fun selectWithoutTransactions(dbHelper: DbHelper) {
  val aList = dbHelper.database.tableAQueries.selectAll().executeAsList()

  val bList = dbHelper.database.tableBQueries.selectAll().executeAsList()

  val cList = dbHelper.database.tableCQueries.selectAll().executeAsList()
}

fun insertSomeData(dbHelper: DbHelper) {
  for (i in 0..100) {
    val a = TableA(0, genString(10), genString(10), genLong())
    dbHelper.database.tableAQueries.insert(a)

    val b = TableB(0, genString(10), genString(10), genLong())
    dbHelper.database.tableBQueries.insert(b)

    val c = TableC(0, genString(10), genString(10), genLong())
    dbHelper.database.tableCQueries.insert(c)
  }
}
