package com.squareup.sqldelight.runtime.coroutines

import app.cash.turbine.test
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.internal.copyOnWriteList
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.flow.take
import kotlin.test.*

class MappingTest : DbTest {

  override suspend fun setupDb(): TestDb = TestDb(testDriver())

  @Test fun mapToOne() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOne()
        .test {
          assertEquals(Employee("alice", "Alice Allison"), expectItem())
          cancel()
        }
  }

  @Test fun mapToOneThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOne()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneThrowsFromQueryExecute() = runTest { db ->
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOne()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneThrowsOnMultipleRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER)
        .asFlow()
        .mapToOne()
        .test {
          val message = expectError().message!!
          assertTrue("ResultSet returned more than 1 row" in message, message)
        }
  }

  @Test fun mapToOneOrDefault() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          assertEquals(Employee("alice", "Alice Allison"), expectItem())
          cancel()
        }
  }

  @Test fun mapToOneOrDefaultThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrDefaultThrowsFromQueryExecute() = runTest { db ->
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrDefaultThrowsOnMultipleRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER) //
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          val message = expectError().message!!
          assertTrue("ResultSet returned more than 1 row" in message, message)
        }
  }

  @Test fun mapToOneOrDefaultReturnsDefaultWhenNoResults() = runTest { db ->
    val defaultEmployee = Employee("fred", "Fred Frederson")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER) //
        .asFlow()
        .mapToOneOrDefault(defaultEmployee)
        .test {
          assertSame(defaultEmployee, expectItem())
          cancel()
        }
  }

  @Test fun mapToList() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .mapToList()
        .test {
          assertEquals(listOf(
              Employee("alice", "Alice Allison"), //
              Employee("bob", "Bob Bobberson"), //
              Employee("eve", "Eve Evenson")
          ), expectItem())
          cancel()
        }
  }

  @Test fun mapToListThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, { throw expected })
        .asFlow()
        .mapToList()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToListThrowsFromQueryExecute() = runTest { db ->
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToList()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToListEmptyWhenNoRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE 1=2", MAPPER)
        .asFlow()
        .mapToList()
        .test {
          assertEquals(emptyList(), expectItem())
          cancel()
        }
  }

  @Test fun mapToOneOrNull() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneOrNull()
        .test {
          assertEquals(Employee("alice", "Alice Allison"), expectItem())
          cancel()
        }
  }

  @Test fun mapToOneOrNullThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOneOrNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrNullThrowsFromQueryExecute() = runTest { db ->
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOneOrNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrNullThrowsOnMultipleRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER) //
        .asFlow()
        .mapToOneOrNull()
        .test {
          val message = expectError().message!!
          assertTrue("ResultSet returned more than 1 row" in message, message)
        }
  }

  @Test fun mapToOneOrNullEmptyWhenNoResults() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER) //
        .asFlow()
        .mapToOneOrNull()
        .test {
          assertNull(expectItem())
          cancel()
        }
  }

  @Test fun mapToOneNonNull() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneNotNull()
        .test {
          assertEquals(Employee("alice", "Alice Allison"), expectItem())
          cancel()
        }
  }

  @Test fun mapToOneNonNullThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOneNotNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneNonNullThrowsFromQueryExecute() = runTest { db ->
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOneNotNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = expectError()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneNonNullDoesNotEmitForNoResults() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER)
        .asFlow()
        .take(1) // Ensure we have an event (complete) that the script can validate.
        .mapToOneNotNull()
        .test {
          expectComplete()
        }
  }
}
