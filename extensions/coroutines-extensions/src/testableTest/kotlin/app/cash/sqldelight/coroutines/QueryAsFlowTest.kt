package app.cash.sqldelight.coroutines

import app.cash.sqldelight.coroutines.Employee.Companion.MAPPER
import app.cash.sqldelight.coroutines.Employee.Companion.SELECT_EMPLOYEES
import app.cash.sqldelight.coroutines.Employee.Companion.USERNAME
import app.cash.sqldelight.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

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

  private class StopException : Exception()

  @Test fun queryCanBeCollectedMoreThanOnce() = runTest { db ->
    val flow = db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE $USERNAME = 'john'", MAPPER)
      .asFlow()
      .mapToOneNotNull(Dispatchers.Default)

    val employee = Employee("john", "John Johnson")

    val jobs = (0..5).map { repeat ->
      async {
        var value: Int? = null
        try {
          flow.collect {
            assertEquals(employee, it)
            value = repeat
            throw StopException()
          }
        } catch (_: StopException) {
        }
        value
      }
    }

    db.employee(employee)
    val result = jobs.awaitAll()
    assertEquals(setOf(0, 1, 2, 3, 4, 5), result.toSet())
  }
}
