package com.squareup.sqldelight.runtime.coroutines

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.internal.copyOnWriteList
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.take
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

@FlowPreview
class MappingTest {
  private val db = TestDb()

  @AfterTest fun tearDown() {
    db.close()
  }

  @Test fun mapToOne() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOne()
        .test {
          assertEquals(Employee("alice", "Alice Allison"), item())
          cancel()
        }
  }

  @Test fun mapToOneThrowsFromMapFunction() = runTest {
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOne()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOne()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneThrowsOnMultipleRows() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER)
        .asFlow()
        .mapToOne()
        .test {
          val message = error().message!!
          assertTrue("ResultSet returned more than 1 row" in message, message)
        }
  }

  @Test fun mapToOneOrDefault() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          assertEquals(Employee("alice", "Alice Allison"), item())
          cancel()
        }
  }

  @Test fun mapToOneOrDefaultThrowsFromMapFunction() = runTest {
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrDefaultThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrDefaultThrowsOnMultipleRows() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER) //
        .asFlow()
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test {
          val message = error().message!!
          assertTrue("ResultSet returned more than 1 row" in message, message)
        }
  }

  @Test fun mapToOneOrDefaultReturnsDefaultWhenNoResults() = runTest {
    val defaultEmployee = Employee("fred", "Fred Frederson")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER) //
        .asFlow()
        .mapToOneOrDefault(defaultEmployee)
        .test {
          assertSame(defaultEmployee, item())
          cancel()
        }
  }

  @Test fun mapToList() = runTest {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .asFlow()
        .mapToList()
        .test {
          assertEquals(listOf(
              Employee("alice", "Alice Allison"), //
              Employee("bob", "Bob Bobberson"), //
              Employee("eve", "Eve Evenson")
          ), item())
          cancel()
        }
  }

  @Test fun mapToListThrowsFromMapFunction() = runTest {
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, { throw expected })
        .asFlow()
        .mapToList()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToListThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToList()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToListEmptyWhenNoRows() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE 1=2", MAPPER)
        .asFlow()
        .mapToList()
        .test {
          assertEquals(emptyList(), item())
          cancel()
        }
  }

  @Test fun mapToOneOrNull() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneOrNull()
        .test {
          assertEquals(Employee("alice", "Alice Allison"), item())
          cancel()
        }
  }

  @Test fun mapToOneOrNullThrowsFromMapFunction() = runTest {
    val expected = IllegalStateException("test exception")
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOneOrNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrNullThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOneOrNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneOrNullThrowsOnMultipleRows() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER) //
        .asFlow()
        .mapToOneOrNull()
        .test {
          val message = error().message!!
          assertTrue("ResultSet returned more than 1 row" in message, message)
        }
  }

  @Test fun mapToOneOrNullEmptyWhenNoResults() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER) //
        .asFlow()
        .mapToOneOrNull()
        .test {
          assertNull(item())
          cancel()
        }
  }

  @Test fun mapToOneNonNull() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .asFlow()
        .mapToOneNotNull()
        .test {
          assertEquals(Employee("alice", "Alice Allison"), item())
          cancel()
        }
  }

  @Test fun mapToOneNonNullThrowsFromMapFunction() = runTest {
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
        .asFlow()
        .mapToOneNotNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneNonNullThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToOneNotNull()
        .test {
          // We can't assertSame because coroutines break exception referential transparency.
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToOneNonNullDoesNotEmitForNoResults() = runTest {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER)
        .asFlow()
        .take(1) // Ensure we have an event (complete) that the script can validate.
        .mapToOneNotNull()
        .test {
          complete()
        }
  }
}
