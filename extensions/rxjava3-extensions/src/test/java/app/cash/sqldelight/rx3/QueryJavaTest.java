package app.cash.sqldelight.rx3;

import app.cash.sqldelight.Query;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Arrays;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;

import static app.cash.sqldelight.rx3.Employee.MAPPER;
import static app.cash.sqldelight.rx3.Employee.SELECT_EMPLOYEES;
import static app.cash.sqldelight.rx3.TestDb.TABLE_EMPLOYEE;

public final class QueryJavaTest {
  private TestDb db = new TestDb();

  @After public void tearDown() {
    db.close();
  }

  @Test public void mapToOne() {
    Query<Employee> query = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER);
    RxQuery.toObservable(query, Schedulers.trampoline())
        .to(RxQuery::mapToOne)
        .test()
        .assertValue(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneOrDefault() {
    Query<Employee> query = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER);
    RxQuery.toObservable(query, Schedulers.trampoline())
        .to(o -> RxQuery.mapToOneOrDefault(o, new Employee("fred", "Fred Frederson")))
        .test()
        .assertValue(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToList() {
    Query<Employee> query = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES, MAPPER);
    RxQuery.toObservable(query, Schedulers.trampoline())
        .to(RxQuery::mapToList)
        .test()
        .assertValue(Arrays.asList(
            new Employee("alice", "Alice Allison"),
            new Employee("bob", "Bob Bobberson"),
            new Employee("eve", "Eve Evenson")
        ));
  }

  @Test public void mapToOptional() {
    Query<Employee> query = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER);
    RxQuery.toObservable(query, Schedulers.trampoline())
        .to(RxQuery::mapToOptional)
        .test()
        .assertValue(Optional.of(new Employee("alice", "Alice Allison")));
  }

  @Test public void mapToOneNonNull() {
    Query<Employee> query = db.createQuery(TABLE_EMPLOYEE, SELECT_EMPLOYEES + " LIMIT 1", MAPPER);
    RxQuery.toObservable(query, Schedulers.trampoline())
        .to(RxQuery::mapToOneNonNull)
        .test()
        .assertValue(new Employee("alice", "Alice Allison"));
  }
}
