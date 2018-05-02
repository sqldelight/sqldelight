package com.squareup.sqldelight.runtime.rx

import com.squareup.sqldelight.runtime.rx.Employee.Companion.MAPPER
import com.squareup.sqldelight.runtime.rx.Employee.Companion.SELECT_EMPLOYEES
import com.squareup.sqldelight.runtime.rx.TestDb.Companion.TABLE_EMPLOYEE
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Optional

class QueryTest {
  private lateinit var db: TestDb

  @Before fun setup() {
    db = TestDb()
  }

  @After fun tearDown() {
    db.close()
  }

  @Test fun mapToOne() {
    db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES LIMIT 1", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToOne()
        .test()
        .assertValue(Employee("alice", "Alice Allison"))
  }

  @Test fun `mapToOne throws on multiple rows`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 2", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToOne()
        .test()
        .assertError { it.message!!.contains("ResultSet returned more than 1 row") }
  }

  @Test fun mapToOneOrDefault() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test()
        .assertValue(Employee("alice", "Alice Allison"))
  }

  @Test fun `mapToOneOrDefault throws on multiple rows`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 2", MAPPER) //
        .observe(Schedulers.trampoline())
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test()
        .assertError { it.message!!.contains("ResultSet returned more than 1 row") }
  }

  @Test fun `mapToOneOrDefault returns default when no results`() {
    val defaultEmployee = Employee("fred", "Fred Frederson")

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 0", MAPPER) //
        .observe(Schedulers.trampoline())
        .mapToOneOrDefault(Employee("fred", "Fred Frederson"))
        .test()
        .assertValue(defaultEmployee)
  }

  @Test fun mapToList() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
        .observe(Schedulers.trampoline())
        .mapToList()
        .test()
        .assertValue(listOf(
            Employee("alice", "Alice Allison"), //
            Employee("bob", "Bob Bobberson"), //
            Employee("eve", "Eve Evenson")
        ))
  }

  @Test fun `mapToList empty when no rows`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " WHERE 1=2", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToList()
        .test()
        .assertValue(emptyList())
  }

  @Test fun mapToOptional() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToOptional()
        .test()
        .assertValue(Optional.of(Employee("alice", "Alice Allison")))
  }

  @Test fun `mapToOptional throws on multiple rows`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 2", MAPPER) //
        .observe(Schedulers.trampoline())
        .mapToOptional()
        .test()
        .assertError { it.message!!.contains("ResultSet returned more than 1 row") }
  }

  @Test fun `mapToOptional empty when no results`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 0", MAPPER) //
        .observe(Schedulers.trampoline())
        .mapToOptional()
        .test()
        .assertValue(Optional.empty())
  }

  @Test fun mapToOneNonNull() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToOneNonNull()
        .test()
        .assertValue(Employee("alice", "Alice Allison"))
  }

  @Test fun `mapToOneNonNull doesnt emit for no results`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 0", MAPPER)
        .observe(Schedulers.trampoline())
        .mapToOneNonNull()
        .test()
        .assertNoValues()
  }
}