package com.squareup.sqldelight.runtime.coroutines

import app.cash.turbine.test
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.USERNAME
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class QueryAsFlowTest : DbTest {

  override suspend fun setupDb(): TestDb = TestDb(testDriver())

  @Test fun query() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .test {
          expectItem().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          cancel()
        }
  }

  @Test fun queryObservesNotification() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .test {
          expectItem().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          db.employee(Employee("john", "John Johnson"))
          expectItem().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
            hasRow("john", "John Johnson")
          }

          cancel()
        }
  }

  @Test fun queryNotNotifiedAfterCancel() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .test {
          expectItem().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          cancel()

          db.employee(Employee("john", "John Johnson"))
          yield() // Ensure any events can be delivered.
          expectNoEvents()
        }
  }

  @Test fun queryOnlyNotifiedAfterCollect() = runTest { db ->
    val flow = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()

    db.employee(Employee("john", "John Johnson"))

    flow.test {
      expectItem().assert {
        hasRow("alice", "Alice Allison")
        hasRow("bob", "Bob Bobberson")
        hasRow("eve", "Eve Evenson")
        hasRow("john", "John Johnson")
      }
      cancel()
    }
  }

  @Test fun queryCanBeCollectedMoreThanOnce() = runTest { db ->
    val flow = db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE $USERNAME = 'john'", MAPPER)
        .asFlow()
        .mapToOneNotNull()

    val employee = Employee("john", "John Johnson")

    repeat(5) {
      launch {
        flow.test {
          assertEquals(employee, expectItem())
          cancel()
        }
      }
    }

    db.employee(employee)
  }
}
