package com.squareup.sqldelight.runtime.coroutines

import co.touchlab.stately.collections.frozenHashMap
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use
import com.squareup.sqldelight.internal.Atomic
import com.squareup.sqldelight.internal.copyOnWriteList
import com.squareup.sqldelight.internal.getValue
import com.squareup.sqldelight.internal.setValue
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_MANAGER

expect suspend fun testDriver(): SqlDriver

class TestDb(
  val db: SqlDriver
) : TransacterImpl(db) {
  val queries = frozenHashMap<String, MutableList<Query<*>>>()

  var aliceId: Long by Atomic<Long>(0)
  var bobId: Long by Atomic<Long>(0)
  var eveId: Long by Atomic<Long>(0)

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
    return object : Query<T>(queries.getOrPut(key, { copyOnWriteList() }), mapper) {
      override fun execute(): SqlCursor {
        return db.executeQuery(null, query, 0)
      }
    }
  }

  fun notify(key: String) {
    queries[key]?.let { notifyQueries(key.hashCode(), { it }) }
  }

  fun close() {
    db.close()
  }

  fun employee(employee: Employee): Long {
    db.execute(
      0,
      """
      |INSERT OR FAIL INTO $TABLE_EMPLOYEE (${Employee.USERNAME}, ${Employee.NAME})
      |VALUES (?, ?)
      |""".trimMargin(),
      2
    ) {
      bindString(1, employee.username)
      bindString(2, employee.name)
    }
    notify(TABLE_EMPLOYEE)
    // last_insert_rowid is connection-specific, so run it in the transaction thread/connection
    return transactionWithResult {
      db.executeQuery(2, "SELECT last_insert_rowid()", 0).use {
        it.next()
        it.getLong(0)!!
      }
    }
  }

  fun manager(
    employeeId: Long,
    managerId: Long
  ): Long {
    db.execute(
      1,
      """
      |INSERT OR FAIL INTO $TABLE_MANAGER (${Manager.EMPLOYEE_ID}, ${Manager.MANAGER_ID})
      |VALUES (?, ?)
      |""".trimMargin(),
      2
    ) {
      bindLong(1, employeeId)
      bindLong(2, managerId)
    }
    notify(TABLE_MANAGER)
    // last_insert_rowid is connection-specific, so run it in the transaction thread/connection
    return transactionWithResult {
      db.executeQuery(2, "SELECT last_insert_rowid()", 0).use {
        it.next()
        it.getLong(0)!!
      }
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
    |""".trimMargin()
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
