package com.squareup.sqldelight.runtime.rx

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.internal.QueryList
import com.squareup.sqldelight.runtime.rx.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.rx.TestDb.Companion.TABLE_EMPLOYEE
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class QueryObservableTest {
  @Test fun mapToListThrowsFromQueryRun() {
    val error = IllegalStateException("test exception")
    val preparedStatement = object : SqlPreparedStatement {
      override fun bindBytes(index: Int, bytes: ByteArray?) = throw AssertionError()
      override fun bindLong(index: Int, long: Long?) = throw AssertionError()
      override fun bindDouble(index: Int, double: Double?) = throw AssertionError()
      override fun bindString(index: Int, string: String?) = throw AssertionError()
      override fun execute() = throw AssertionError()

      override fun executeQuery(): SqlResultSet {
        throw error
      }
    }

    val query = Query<Any>(preparedStatement, QueryList(), {
      throw AssertionError("Must not be called")
    })

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
