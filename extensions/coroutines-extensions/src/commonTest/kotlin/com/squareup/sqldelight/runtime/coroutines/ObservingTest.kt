package com.squareup.sqldelight.runtime.coroutines

import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.USERNAME
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.flow.zip
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@FlowPreview
class ObservingTest {
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

  @Ignore // Cannot be validated on all platforms without a test dispatcher.
  @Test fun queryInitialValueAndTriggerUsesScheduler() = runTest {
    val testDispatcher = TODO()
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow(testDispatcher)
        .test {
          noEvents()

          // testDispatcher.triggerEvents()
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
          }

          db.employee(Employee("john", "John Johnson"))
          noEvents()

          // testDispatcher.triggerEvents()
          item().assert {
            hasRow("alice", "Alice Allison")
            hasRow("bob", "Bob Bobberson")
            hasRow("eve", "Eve Evenson")
            hasRow("john", "John Johnson")
          }

          cancel()
        }
  }

  @Test fun queryNotNotifiedAfterDispose() = runTest {
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

  @Test fun queryOnlyNotifiedAfterSubscribe() = runTest {
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

  @Test fun queryCanBeSubscribedToTwice() = runTest {
    val flow = db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE $USERNAME = 'john'", MAPPER)
        .asFlow()
        .mapToOneNonNull()

    flow.zip(flow) { one, two -> one to two }
        .test {
          val employee = Employee("john", "John Johnson")
          db.employee(employee)
          assertEquals(employee to employee, item())

          cancel()
        }
  }
}
