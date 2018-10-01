package com.squareup.sqldelight.runtime.rx

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXEC
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.internal.QueryList
import com.squareup.sqldelight.runtime.rx.TestDb.Companion.TABLE_EMPLOYEE
import com.squareup.sqldelight.runtime.rx.TestDb.Companion.TABLE_MANAGER
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper

class TestDb(
  val helper: SqlDatabase = SqliteJdbcOpenHelper(),
  val db: SqlDatabaseConnection = helper.getConnection()
) : Transacter(helper) {
  val queries = mutableMapOf<String, QueryList>()

  var aliceId: Long = 0
  var bobId: Long = 0
  var eveId: Long = 0

  init {
    db.prepareStatement("PRAGMA foreign_keys=ON", EXEC, 0).execute()

    db.prepareStatement(CREATE_EMPLOYEE, EXEC, 0).execute()
    aliceId = employee(Employee("alice", "Alice Allison"))
    bobId = employee(Employee("bob", "Bob Bobberson"))
    eveId = employee(Employee("eve", "Eve Evenson"))

    db.prepareStatement(CREATE_MANAGER, EXEC, 0).execute()
    manager(eveId, aliceId)
  }

  fun <T: Any> createQuery(key: String, query: String, mapper: (SqlCursor) -> T): Query<T> {
    return object : Query<T>(queries.getOrPut(key, ::QueryList), mapper) {
      override fun createStatement(): SqlPreparedStatement {
        return db.prepareStatement(query, SELECT, 0)
      }
    }
  }

  fun notify(key: String) {
    queries[key]?.let { notifyQueries(it) }
  }

  fun close() {
    helper.close()
  }

  fun employee(employee: Employee): Long {
    val statement = db.prepareStatement("""
      |INSERT OR FAIL INTO $TABLE_EMPLOYEE (${Employee.USERNAME}, ${Employee.NAME})
      |VALUES (?, ?)
      |""".trimMargin(), INSERT, 2)
    statement.bindString(1, employee.username)
    statement.bindString(2, employee.name)
    val result = statement.execute()
    notify(TABLE_EMPLOYEE)
    return result
  }

  fun manager(
    employeeId: Long,
    managerId: Long
  ): Long {
    val statement = db.prepareStatement("""
      |INSERT OR FAIL INTO $TABLE_MANAGER (${Manager.EMPLOYEE_ID}, ${Manager.MANAGER_ID})
      |VALUES (?, ?)
      |""".trimMargin(), INSERT, 2)
    statement.bindLong(1, employeeId)
    statement.bindLong(2, managerId)
    val result = statement.execute()
    notify(TABLE_MANAGER)
    return result
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
    |""".trimMargin()
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
