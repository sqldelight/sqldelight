package com.squareup.sqldelight.runtime.rx

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.internal.copyOnWriteList
import com.squareup.sqldelight.runtime.rx.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.rx.TestDb.Companion.TABLE_EMPLOYEE
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class QueryObservableTest {
  @Test fun mapToListThrowsFromQueryRun() {
    val error = IllegalStateException("test exception")

    val query = object : Query<Any>(copyOnWriteList(), { throw AssertionError("Must not be called") }) {
      override fun execute() = throw error
    }

    query.asObservable(Schedulers.trampoline()).mapToList()
        .test()
        .assertNoValues()
        .assertError(error)
  }

  @Test fun mapToListThrowsFromMapFunction() {
    val db = TestDb()
    val error = IllegalStateException("test exception")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, { throw error })
        .asObservable(Schedulers.trampoline())
        .mapToList()
        .test()
        .assertNoValues()
        .assertError(error)

    db.close()
  }
}
