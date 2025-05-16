package app.cash.sqldelight.rx2

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import app.cash.sqldelight.rx2.Employee.Companion
import app.cash.sqldelight.rx2.TestDb.Companion.TABLE_EMPLOYEE
import app.cash.sqldelight.rx2.TestDb.Companion.TABLE_MANAGER

class TestDb(
  val db: SqlDriver = JdbcSqliteDriver(IN_MEMORY),
) : TransacterImpl(db) {
  var aliceId: Long = 0
  var bobId: Long = 0
  var eveId: Long = 0

  init {
    db.execute(null, "PRAGMA foreign_keys=ON", 0)

    db.execute(null, CREATE_EMPLOYEE, 0)
    aliceId = employee(Employee("alice", "Alice Allison"))
    bobId = employee(Employee("bob", "Bob Bobberson"))
    eveId = employee(Employee("eve", "Eve Evenson"))

    db.execute(null, CREATE_MANAGER, 0)
    manager(eveId, aliceId)
  }

  fun <T : Any> createQuery(key: String, query: String, mapper: (SqlCursor) -> T): Query<T> {
    return object : Query<T>(mapper) {
      override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return db.executeQuery(null, query, mapper, 0)
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

  fun employee(employee: Employee): Long {
    db.execute(
      0,
      """
      |INSERT OR FAIL INTO $TABLE_EMPLOYEE (${app.cash.sqldelight.rx2.Employee.USERNAME}, ${app.cash.sqldelight.rx2.Employee.NAME})
      |VALUES (?, ?)
      |
      """.trimMargin(),
      2,
    ) {
      bindString(0, employee.username)
      bindString(1, employee.name)
    }
    notify(TABLE_EMPLOYEE)
    return db.executeQuery(2, "SELECT last_insert_rowid()", ::getLong, 0).value
  }

  fun manager(
    employeeId: Long,
    managerId: Long,
  ): Long {
    db.execute(
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
    return db.executeQuery(2, "SELECT last_insert_rowid()", ::getLong, 0).value
  }

  companion object {
    const val TABLE_EMPLOYEE = "employee"
    const val TABLE_MANAGER = "manager"

    val CREATE_EMPLOYEE = """
      |CREATE TABLE $TABLE_EMPLOYEE (
      |  ${app.cash.sqldelight.rx2.Employee.ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  ${app.cash.sqldelight.rx2.Employee.USERNAME} TEXT NOT NULL UNIQUE,
      |  ${app.cash.sqldelight.rx2.Employee.NAME} TEXT NOT NULL
      |)
    """.trimMargin()

    val CREATE_MANAGER = """
      |CREATE TABLE $TABLE_MANAGER (
      |  ${Manager.ID} INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  ${Manager.EMPLOYEE_ID} INTEGER NOT NULL UNIQUE REFERENCES $TABLE_EMPLOYEE(${app.cash.sqldelight.rx2.Employee.ID}),
      |  ${Manager.MANAGER_ID} INTEGER NOT NULL REFERENCES $TABLE_EMPLOYEE(${app.cash.sqldelight.rx2.Employee.ID})
      |)
    """.trimMargin()
  }
}

object Manager {
  const val ID = "id"
  const val EMPLOYEE_ID = "employee_id"
  const val MANAGER_ID = "manager_id"

  val SELECT_MANAGER_LIST = """
    |SELECT e.${Companion.NAME}, m.${Companion.NAME}
    |FROM $TABLE_MANAGER AS manager
    |JOIN $TABLE_EMPLOYEE AS e
    |ON manager.$EMPLOYEE_ID = e.${Companion.ID}
    |JOIN $TABLE_EMPLOYEE AS m
    |ON manager.$MANAGER_ID = m.${Companion.ID}
    |
  """.trimMargin()
}

data class Employee(val username: String, val name: String) {
  companion object {
    const val ID = "id"
    const val USERNAME = "username"
    const val NAME = "name"

    const val SELECT_EMPLOYEES = "SELECT $USERNAME, $NAME FROM $TABLE_EMPLOYEE"

    @JvmField
    val MAPPER = { cursor: SqlCursor ->
      Employee(cursor.getString(0)!!, cursor.getString(1)!!)
    }
  }
}

private fun getLong(cursor: SqlCursor): QueryResult<Long> {
  check(cursor.next().value)
  return QueryResult.Value(cursor.getLong(0)!!)
}
