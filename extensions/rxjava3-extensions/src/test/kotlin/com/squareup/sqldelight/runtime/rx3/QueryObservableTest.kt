package com.squareup.sqldelight.runtime.rx3

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.internal.copyOnWriteList
import com.squareup.sqldelight.runtime.rx3.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.rx3.TestDb.Companion.TABLE_EMPLOYEE
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Test

class QueryObservableTest {
  @Test fun mapToListThrowsFromQueryRun() {
    val error = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { throw AssertionError("Must not be called") }) {
      override fun <R> execute(mapper: (SqlCursor) -> R) = throw error
    }

    query.asObservable(Schedulers.trampoline()).mapToList()
      .test()
      .assertNoValues()
      .assertError(error)
  }

  @Test fun mapToListThrowsFromMapFunction() {
    val db = TestDb()
    val error = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES) { throw error }
      .asObservable(Schedulers.trampoline())
      .mapToList()
      .test()
      .assertNoValues()
      .assertError(error)

    db.close()
  }
}
