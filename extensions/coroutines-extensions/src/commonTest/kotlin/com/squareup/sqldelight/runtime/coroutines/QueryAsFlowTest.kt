package com.squareup.sqldelight.runtime.coroutines

import app.cash.turbine.test
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.USERNAME
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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

  @Test fun queryExecuteAsFlow() = runTest { db ->
    val data = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .executeAsFlow()
      .take(2)
      .toList()
    val expected = listOf(
      Employee(username = "alice", name = "Alice Allison"),
      Employee(username = "bob", name = "Bob Bobberson")
    )
    assertEquals(expected, actual = data)
  }

  @Test fun ensureExecuteAsFlowCallsClose() = runTest {
    var calledClose = false
    var numberOfNextCalls = 0
    val initNextCall = 1

    val fakeQuery = object : Query<Employee>(mutableListOf(), mapper = MAPPER) {
      override fun execute(): SqlCursor = object : SqlCursor {
        override fun close() {
          calledClose = true
        }

        override fun next(): Boolean {
          numberOfNextCalls++
          return numberOfNextCalls < initNextCall
        }

        override fun getString(index: Int) = TODO("Not yet implemented")
        override fun getLong(index: Int) = TODO("Not yet implemented")
        override fun getBytes(index: Int) = TODO("Not yet implemented")
        override fun getDouble(index: Int) = TODO("Not yet implemented")
      }
    }
    fakeQuery.executeAsFlow().collect()
    assertTrue(calledClose)
    assertEquals(1, numberOfNextCalls)
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
