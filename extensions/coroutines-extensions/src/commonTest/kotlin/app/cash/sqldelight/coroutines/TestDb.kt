package app.cash.sqldelight.coroutines

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import app.cash.sqldelight.coroutines.TestDb.Companion.TABLE_MANAGER
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicLong
import co.touchlab.stately.concurrency.value

expect suspend fun testDriver(): SqlDriver

class TestDb(
  val db: SqlDriver,
) : SuspendingTransacterImpl(db) {
  var aliceId = AtomicLong(0)
  var bobId = AtomicLong(0)
  var eveId = AtomicLong(0)

  var isInitialized = AtomicBoolean(false)

  private suspend fun init() {
    db.execute(null, "PRAGMA foreign_keys=ON", 0).await()

    db.execute(null, CREATE_EMPLOYEE, 0).await()
    aliceId.value = employee(Employee("alice", "Alice Allison"))
    bobId.value = employee(Employee("bob", "Bob Bobberson"))
    eveId.value = employee(Employee("eve", "Eve Evenson"))

    db.execute(null, CREATE_MANAGER, 0).await()
    manager(eveId.value, aliceId.value)
  }

  suspend fun use(block: suspend (TestDb) -> Unit) {
    if (!isInitialized.value) {
      init()
    }

    block(this)
  }

  fun <T : Any> createQuery(key: String, query: String, mapper: (SqlCursor) -> T): Query<T> {
    return object : Query<T>(mapper) {
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return db.executeQuery(null, query, mapper, 0, null)
      }

      override fun addListener(listener: Listener) {
        db.addListener(key, listener = listener)
      }

      override fun removeListener(listener: Listener) {
        db.removeListener(key, listener = listener)
      }
    }
  }

  fun notify(key: String) {
    db.notifyListeners(key)
  }

  fun close() {
    db.close()
  }

  suspend fun employee(employee: Employee): Long {
    db.await(
      0,
      """
      |INSERT OR FAIL INTO $TABLE_EMPLOYEE (${Employee.USERNAME}, ${Employee.NAME})
      |VALUES (?, ?)
      |
      """.trimMargin(),
      2,
    ) {
      bindString(0, employee.username)
      bindString(1, employee.name)
    }
    notify(TABLE_EMPLOYEE)
    // last_insert_rowid is connection-specific, so run it in the transaction thread/connection
    return transactionWithResult {
      val mapper: (SqlCursor) -> QueryResult<Long> = {
        it.next()
        QueryResult.Value(it.getLong(0)!!)
      }
      db.executeQuery(2, "SELECT last_insert_rowid()", mapper, 0).await()
    }
  }

  suspend fun manager(
    employeeId: Long,
    managerId: Long,
  ): Long {
    db.await(
      1,
      """
      |INSERT OR FAIL INTO $TABLE_MANAGER (${Manager.EMPLOYEE_ID}, ${Manager.MANAGER_ID})
      |VALUES (?, ?)
      |
      """.trimMargin(),
      2,
    ) {
      bindLong(0, employeeId)
      bindLong(1, managerId)
    }
    notify(TABLE_MANAGER)
    // last_insert_rowid is connection-specific, so run it in the transaction thread/connection
    return transactionWithResult {
      val mapper: (SqlCursor) -> QueryResult<Long> = {
        it.next()
        QueryResult.Value(it.getLong(0)!!)
      }
      db.executeQuery(2, "SELECT last_insert_rowid()", mapper, 0).await()
    }
  }

  companion object {
    const val TABLE_EMPLOYEE = "employee"
    const val TABLE_MANAGER = "manager"

    val CREATE_EMPLOYEE = """
      |CREATE TABLE $TABLE_EMPLOYEE (
      |  ${Employee.ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  ${Employee.USERNAME} TEXT NOT NULL UNIQUE,
      |  ${Employee.NAME} TEXT NOT NULL
      |)
    """.trimMargin()

    val CREATE_MANAGER = """
      |CREATE TABLE $TABLE_MANAGER (
      |  ${Manager.ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  ${Manager.EMPLOYEE_ID} INTEGER NOT NULL UNIQUE REFERENCES $TABLE_EMPLOYEE(${Employee.ID}),
      |  ${Manager.MANAGER_ID} INTEGER NOT NULL REFERENCES $TABLE_EMPLOYEE(${Employee.ID})
      |)
    """.trimMargin()
  }
}

object Manager {
  const val ID = "id"
  const val EMPLOYEE_ID = "employee_id"
  const val MANAGER_ID = "manager_id"

  val SELECT_MANAGER_LIST = """
    |SELECT e.${Employee.NAME}, m.${Employee.NAME}
    |FROM $TABLE_MANAGER AS manager
    |JOIN $TABLE_EMPLOYEE AS e
    |ON manager.$EMPLOYEE_ID = e.${Employee.ID}
    |JOIN $TABLE_EMPLOYEE AS m
    |ON manager.$MANAGER_ID = m.${Employee.ID}
    |
  """.trimMargin()
}

data class Employee(val username: String, val name: String) {
  companion object {
    const val ID = "id"
    const val USERNAME = "username"
    const val NAME = "name"

    const val SELECT_EMPLOYEES = "SELECT $USERNAME, $NAME FROM $TABLE_EMPLOYEE"

    val MAPPER = { cursor: SqlCursor ->
      Employee(cursor.getString(0)!!, cursor.getString(1)!!)
    }
  }
}
