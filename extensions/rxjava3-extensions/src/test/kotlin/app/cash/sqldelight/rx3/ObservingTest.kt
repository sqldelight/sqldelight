package app.cash.sqldelight.rx3

import app.cash.sqldelight.rx3.Employee.Companion.MAPPER
import app.cash.sqldelight.rx3.Employee.Companion.SELECT_EMPLOYEES
import app.cash.sqldelight.rx3.Employee.Companion.USERNAME
import app.cash.sqldelight.rx3.TestDb.Companion.TABLE_EMPLOYEE
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.After
import org.junit.Before
import org.junit.Test

class ObservingTest {
  private val o = RecordingObserver()

  private lateinit var db: TestDb

  @Before fun setup() {
    db = TestDb()
  }

  @After fun tearDown() {
    o.assertNoMoreEvents()
    db.close()
  }

  @Test fun query() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asObservable(Schedulers.trampoline())
      .subscribe(o)
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .isExhausted()
  }

  @Test fun `query observes notification`() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asObservable(Schedulers.trampoline())
      .subscribe(o)
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .isExhausted()

    db.employee(Employee("john", "John Johnson"))
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .hasRow("john", "John Johnson")
      .isExhausted()
  }

  @Test fun queryInitialValueAndTriggerUsesScheduler() {
    val scheduler = TestScheduler()
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asObservable(scheduler)
      .subscribe(o)
    o.assertNoMoreEvents()

    scheduler.triggerActions()
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .isExhausted()

    db.employee(Employee("john", "John Johnson"))
    o.assertNoMoreEvents()
    scheduler.triggerActions()
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .hasRow("john", "John Johnson")
      .isExhausted()
  }

  @Test fun queryNotNotifiedWhenQueryTransformerUnsubscribes() {
    val killSwitch = PublishSubject.create<Any>()

    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asObservable(Schedulers.trampoline())
      .takeUntil(killSwitch)
      .subscribe(o)
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .isExhausted()

    killSwitch.onNext("kill")
    o.assertIsCompleted()

    db.employee(Employee("john", "John Johnson"))
    o.assertNoMoreEvents()
  }

  @Test fun queryNotNotifiedAfterDispose() {
    db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asObservable(Schedulers.trampoline())
      .subscribe(o)
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .isExhausted()
    o.dispose()

    db.employee(Employee("john", "John Johnson"))
    o.assertNoMoreEvents()
  }

  @Test fun queryOnlyNotifiedAfterSubscribe() {
    val query = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER)
      .asObservable(Schedulers.trampoline())
    o.assertNoMoreEvents()

    db.employee(Employee("john", "John Johnson"))
    o.assertNoMoreEvents()

    query.subscribe(o)
    o.assertResultSet()
      .hasRow("alice", "Alice Allison")
      .hasRow("bob", "Bob Bobberson")
      .hasRow("eve", "Eve Evenson")
      .hasRow("john", "John Johnson")
      .isExhausted()
  }

  @Test fun queryCanBeSubscribedToTwice() {
    val query = db.createQuery(TABLE_EMPLOYEE, "$SELECT_EMPLOYEES WHERE $USERNAME = 'john'", MAPPER)
      .asObservable(Schedulers.trampoline())
      .mapToOneNonNull()

    val testObserver = query.zipWith(query, BiFunction { one: Employee, two: Employee -> one to two })
      .test()

    testObserver.assertNoValues()

    val employee = Employee("john", "John Johnson")

    db.employee(employee)
    testObserver.assertValue(employee to employee)
  }
}
