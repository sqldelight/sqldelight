package app.cash.sqldelight.coroutines

import app.cash.sqldelight.coroutines.Employee.Companion.MAPPER
import app.cash.sqldelight.coroutines.Employee.Companion.SELECT_EMPLOYEES
import app.cash.sqldelight.coroutines.Employee.Companion.USERNAME
import app.cash.sqldelight.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryAsFlowTest : DbTest {

  override suspend fun setupDb(): TestDb = TestDb(testDriver())

  @Test fun query() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asFlow()
      .test {
        awaitItem().assert {
          hasRow("alice", "Alice Allison")
          hasRow("bob", "Bob Bobberson")
          hasRow("eve", "Eve Evenson")
        }

        cancel()
      }
  }

  @Test fun queryEmitsWithoutSuspending() = runTest { db ->
    val flow = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER).asFlow()

    var seenValue = false
    val collectJob = launch(start = UNDISPATCHED) {
      flow.collect {
        seenValue = true
      }
    }
    assertTrue(seenValue)
    collectJob.cancel()
  }

  @Test fun queryObservesNotification() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asFlow()
      .test {
        awaitItem().assert {
          hasRow("alice", "Alice Allison")
          hasRow("bob", "Bob Bobberson")
          hasRow("eve", "Eve Evenson")
        }

        db.employee(Employee("john", "John Johnson"))
        awaitItem().assert {
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
        awaitItem().assert {
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
      awaitItem().assert {
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

    val timesCancelled = AtomicInt(0)
    repeat(5) {
      launch {
        flow.test {
          assertEquals(employee, awaitItem())
          cancel()
          timesCancelled.increment()
        }
      }
    }

    db.employee(employee)
    while (timesCancelled.value != 5) {
      yield()
    }
  }
}
