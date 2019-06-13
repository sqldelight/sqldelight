package com.squareup.sqldelight.runtime.coroutines

import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.USERNAME
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.zip
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class QueryAsFlowTest {
  private lateinit var db: TestDb

  @BeforeTest fun setup() {
    db = TestDb()
  }

  @AfterTest fun tearDown() {
    db.close()
  }

  @Test fun query() = runTest {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .test {
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          cancel()
        }
  }

  @Test fun queryObservesNotification() = runTest {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .test {
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          db.employee(Employee("john", "John Johnson"))
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
            hasRow("john", "John Johnson")
          }

          cancel()
        }
  }

  @Test fun queryNotNotifiedAfterCancel() = runTest {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .test {
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          cancel()

          db.employee(Employee("john", "John Johnson"))
          noMoreEvents()
        }
  }

  @Test fun queryOnlyNotifiedAfterCollect() = runTest {
    val flow = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()

    db.employee(Employee("john", "John Johnson"))

    flow.test {
      item().assert {
        hasRow("alice", "Alice Allison")
        hasRow("bob", "Bob Bobberson")
        hasRow("eve", "Eve Evenson")
        hasRow("john", "John Johnson")
      }
      cancel()
    }
  }

  @Test fun queryCanBeCollectedToTwice() = runTest {
    val flow = db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE $USERNAME = 'john'", MAPPER)
        .asFlow()
        .mapToOneNotNull()

    flow.zip(flow) { one, two -> one to two }
        .test {
          val employee = Employee("john", "John Johnson")
          db.employee(employee)
          assertEquals(employee to employee, item())

          cancel()
        }
  }
}
