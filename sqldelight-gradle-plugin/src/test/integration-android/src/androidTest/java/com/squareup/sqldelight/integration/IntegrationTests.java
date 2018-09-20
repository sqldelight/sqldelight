package com.squareup.sqldelight.integration;

import android.support.test.InstrumentationRegistry;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import com.squareup.sqldelight.android.SqlDelight;
import com.squareup.sqldelight.db.SqlDatabase;
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
  private SqlDatabase database;

  private PersonQueries personQueries;
  private SqliteKeywordsQueries keywordsQueries;

  @Before public void before() {
    database = SqlDelight.create(QueryWrapper.Helper, InstrumentationRegistry.getContext());
    queryWrapper = new QueryWrapper(database);
    personQueries = queryWrapper.getPersonQueries();
    keywordsQueries = queryWrapper.getSqliteKeywordsQueries();
  }

  @After public void after() throws IOException {
    database.close();
  }

  @Test public void indexedArgs() {
    // ?1 is the only arg
    Person person = personQueries.equivalentNames("Bob", AutoValue_MyPerson::new).executeAsOne();
    assertThat(person).isEqualTo(new AutoValue_MyPerson(4, "Bob", "Bob"));
  }

  @Test public void startIndexAtTwo() {
    // ?2 is the only arg
    Person person = personQueries.equivalentNames2("Bob", AutoValue_MyPerson::new).executeAsOne();
    assertThat(person).isEqualTo(new AutoValue_MyPerson(4, "Bob", "Bob"));
  }

  @Test public void namedIndexArgs() {
    // :name is the only arg
    Person person = personQueries.equivalentNamesNamed("Bob", AutoValue_MyPerson::new).executeAsOne();
    assertThat(person).isEqualTo(new AutoValue_MyPerson(4, "Bob", "Bob"));
  }

  @Test public void indexedArgLast() {
    // First arg declared is ?, second arg declared is ?1.
    Person person = personQueries.indexedArgLast("Bob", AutoValue_MyPerson::new).executeAsOne();
    assertThat(person).isEqualTo(new AutoValue_MyPerson(4, "Bob", "Bob"));
  }

  @Test public void indexedArgLastTwo() {
    // First arg declared is ?, second arg declared is ?2.
    Person person = personQueries.indexedArgLast2("Alec", "Strong", AutoValue_MyPerson::new).executeAsOne();
    assertThat(person).isEqualTo(new AutoValue_MyPerson(1, "Alec", "Strong"));
  }

  @Test public void nameIn() {
    List<Person> people = personQueries.nameIn(Arrays.asList("Alec", "Matt", "Jake")).executeAsList();
    assertThat(people).hasSize(3);
  }

  @Test public void sqliteKeywordQuery() {
    SqliteKeywords keywords = keywordsQueries.selectAll(AutoValue_SqliteKeywords::new).executeAsOne();
    assertThat(keywords).isEqualTo(new AutoValue_SqliteKeywords(1, 10, 20));
  }

  @Test public void compiledStatement() {
    keywordsQueries.insertStmt(11, 21);
    keywordsQueries.insertStmt(12, 22);

    long current = 10;
    for (_group_ group : keywordsQueries.selectAll().executeAsList()) {
      assertThat(group.get_where_()).isEqualTo(current++);
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
    for (_group_ group : keywordsQueries.selectAll().executeAsList()) {
      assertThat(group.get_where_()).isEqualTo(current++);
    }
    assertThat(current).isEqualTo(13);
  }
}
