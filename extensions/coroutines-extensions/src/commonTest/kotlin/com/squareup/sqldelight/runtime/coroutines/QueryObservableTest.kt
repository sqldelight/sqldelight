package com.squareup.sqldelight.runtime.coroutines

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.internal.copyOnWriteList
import com.squareup.sqldelight.runtime.coroutines.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.coroutines.TestDb.Companion.TABLE_EMPLOYEE
import kotlinx.coroutines.FlowPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.fail

@FlowPreview
class QueryObservableTest {
  @Test fun mapToListThrowsFromQueryRun() = runTest {
    val expected = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { fail() }) {
      override fun execute() = throw expected
    }

    query.asFlow()
        .mapToList()
        .test {
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }

  @Test fun mapToListThrowsFromMapFunction() = runTest {
    val db = TestDb()
    val expected = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, { throw expected })
        .asFlow()
        .mapToList()
        .test {
          val actual = error()
          assertEquals(IllegalStateException::class, actual::class)
          assertEquals(expected.message, actual.message)
        }
  }
}
