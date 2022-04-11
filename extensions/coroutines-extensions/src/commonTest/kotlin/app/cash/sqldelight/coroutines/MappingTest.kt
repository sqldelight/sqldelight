package app.cash.sqldelight.coroutines

import app.cash.sqldelight.Query
import app.cash.turbine.test
import app.cash.sqldelight.coroutines.Employee.Companion.MAPPER
import app.cash.sqldelight.coroutines.Employee.Companion.SELECT_EMPLOYEES
import app.cash.sqldelight.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.flow.take
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class MappingTest : DbTest {

  override suspend fun setupDb(): TestDb = TestDb(testDriver())

  @Test
  fun mapToOne() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
      .asFlow()
      .mapToOne(coroutineContext)
      .test {
        assertEquals(Employee("alice", "Alice Allison"), awaitItem())
        cancel()
      }
  }

  @Test fun mapToOneThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
      .asFlow()
      .mapToOne(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneThrowsFromQueryExecute() = runTest { _ ->
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>({ fail() }) {
      override fun execute() = throw expected
      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }

    query.asFlow()
      .mapToOne(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneThrowsOnMultipleRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER)
      .asFlow()
      .mapToOne(coroutineContext)
      .test {
        val message = awaitError().message!!
        assertTrue("ResultSet returned more than 1 row" in message, message)
      }
  }

  @Test fun mapToOneOrDefault() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
      .asFlow()
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), coroutineContext)
      .test {
        assertEquals(Employee("alice", "Alice Allison"), awaitItem())
        cancel()
      }
  }

  @Test fun mapToOneOrDefaultThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
      .asFlow()
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneOrDefaultThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>({ fail() }) {
      override fun execute() = throw expected
      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }

    query.asFlow()
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneOrDefaultThrowsOnMultipleRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER) //
      .asFlow()
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), coroutineContext)
      .test {
        val message = awaitError().message!!
        assertTrue("ResultSet returned more than 1 row" in message, message)
      }
  }

  @Test fun mapToOneOrDefaultReturnsDefaultWhenNoResults() = runTest { db ->
    val defaultEmployee = Employee("fred", "Fred Frederson")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER) //
      .asFlow()
      .mapToOneOrDefault(defaultEmployee, coroutineContext)
      .test {
        assertSame(defaultEmployee, awaitItem())
        cancel()
      }
  }

  @Test fun mapToList() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asFlow()
      .mapToList(coroutineContext)
      .test {
        assertEquals(
          listOf(
            Employee("alice", "Alice Allison"), //
            Employee("bob", "Bob Bobberson"), //
            Employee("eve", "Eve Evenson")
          ),
          awaitItem()
        )
        cancel()
      }
  }

  @Test fun mapToListThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, { throw expected })
      .asFlow()
      .mapToList(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToListThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>({ fail() }) {
      override fun execute() = throw expected
      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }

    query.asFlow()
      .mapToList(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToListEmptyWhenNoRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE 1=2", MAPPER)
      .asFlow()
      .mapToList(coroutineContext)
      .test {
        assertEquals(emptyList(), awaitItem())
        cancel()
      }
  }

  @Test fun mapToOneOrNull() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
      .asFlow()
      .mapToOneOrNull(coroutineContext)
      .test {
        assertEquals(Employee("alice", "Alice Allison"), awaitItem())
        cancel()
      }
  }

  @Test fun mapToOneOrNullThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
      .asFlow()
      .mapToOneOrNull(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneOrNullThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>({ fail() }) {
      override fun execute() = throw expected
      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }

    query.asFlow()
      .mapToOneOrNull(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneOrNullThrowsOnMultipleRows() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 2", MAPPER) //
      .asFlow()
      .mapToOneOrNull(coroutineContext)
      .test {
        val message = awaitError().message!!
        assertTrue("ResultSet returned more than 1 row" in message, message)
      }
  }

  @Test fun mapToOneOrNullEmptyWhenNoResults() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER) //
      .asFlow()
      .mapToOneOrNull(coroutineContext)
      .test {
        assertNull(awaitItem())
        cancel()
      }
  }

  @Test fun mapToOneNonNull() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
      .asFlow()
      .mapToOneNotNull(coroutineContext)
      .test {
        assertEquals(Employee("alice", "Alice Allison"), awaitItem())
        cancel()
      }
  }

  @Test fun mapToOneNonNullThrowsFromMapFunction() = runTest { db ->
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", { throw expected })
      .asFlow()
      .mapToOneNotNull(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneNonNullThrowsFromQueryExecute() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>({ fail() }) {
      override fun execute() = throw expected
      override fun addListener(listener: Listener) = Unit
      override fun removeListener(listener: Listener) = Unit
    }

    query.asFlow()
      .mapToOneNotNull(coroutineContext)
      .test {
        // We can't assertSame because coroutines break exception referential transparency.
        val actual = awaitError()
        assertEquals(IllegalStateException::class, actual::class)
        assertEquals(expected.message, actual.message)
      }
  }

  @Test fun mapToOneNonNullDoesNotEmitForNoResults() = runTest { db ->
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 0", MAPPER)
      .asFlow()
      .take(1) // Ensure we have an event (complete) that the script can validate.
      .mapToOneNotNull(coroutineContext)
      .test {
        awaitComplete()
      }
  }
}
