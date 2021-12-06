package app.cash.sqldelight.integration;

import androidx.test.InstrumentationRegistry;
import app.cash.sqldelight.db.SqlDriver;
import app.cash.sqldelight.driver.android.AndroidSqliteDriver;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class IntegrationTests {
  private QueryWrapper queryWrapper;
  private SqlDriver database;

  private PersonQueries personQueries;
  private SqliteKeywordsQueries keywordsQueries;

  @Before public void before() {
    database = new AndroidSqliteDriver(QueryWrapper.Companion.getSchema(), InstrumentationRegistry.getContext());
    queryWrapper = QueryWrapper.Companion.invoke(database);
    personQueries = queryWrapper.getPersonQueries();
    keywordsQueries = queryWrapper.getSqliteKeywordsQueries();
  }

  @After public void after() throws IOException {
    database.close();
  }

  @Test public void indexedArgs() {
    // ?1 is the only arg
    Person person = personQueries.equivalentNames("Bob").executeAsOne();
    assertThat(person).isEqualTo(new Person(4, "Bob", "Bob"));
  }

  @Test public void startIndexAtTwo() {
    // ?2 is the only arg
    Person person = personQueries.equivalentNames2("Bob").executeAsOne();
    assertThat(person).isEqualTo(new Person(4, "Bob", "Bob"));
  }

  @Test public void namedIndexArgs() {
    // :name is the only arg
    Person person = personQueries.equivalentNamesNamed("Bob").executeAsOne();
    assertThat(person).isEqualTo(new Person(4, "Bob", "Bob"));
  }

  @Test public void indexedArgLast() {
    // First arg declared is ?, second arg declared is ?1.
    Person person = personQueries.indexedArgLast("Bob").executeAsOne();
    assertThat(person).isEqualTo(new Person(4, "Bob", "Bob"));
  }

  @Test public void indexedArgLastTwo() {
    // First arg declared is ?, second arg declared is ?2.
    Person person = personQueries.indexedArgLast2("Alec", "Strong").executeAsOne();
    assertThat(person).isEqualTo(new Person(1, "Alec", "Strong"));
  }

  @Test public void nameIn() {
    List<Person> people = personQueries.nameIn(Arrays.asList("Alec", "Matt", "Jake")).executeAsList();
    assertThat(people).hasSize(3);
  }

  @Test public void sqliteKeywordQuery() {
    Group keywords = keywordsQueries.selectAll().executeAsOne();
    assertThat(keywords).isEqualTo(new Group(1, 10, 20));
  }

  @Test public void compiledStatement() {
    keywordsQueries.insertStmt(11, 21);
    keywordsQueries.insertStmt(12, 22);

    long current = 10;
    for (Group group : keywordsQueries.selectAll().executeAsList()) {
      assertThat(group.getWhere_()).isEqualTo(current++);
    }
    assertThat(current).isEqualTo(13);
  }

  @Test public void compiledStatementAcrossThread() throws InterruptedException {
    keywordsQueries.insertStmt(11, 21);

    final CountDownLatch latch = new CountDownLatch(1);
    new Thread(new Runnable() {
      @Override public void run() {
        keywordsQueries.insertStmt(12, 22);
        latch.countDown();
      }
    }).start();

    assertTrue(latch.await(10, SECONDS));

    long current = 10;
    for (Group group : keywordsQueries.selectAll().executeAsList()) {
      assertThat(group.getWhere_()).isEqualTo(current++);
    }
    assertThat(current).isEqualTo(13);
  }
}
