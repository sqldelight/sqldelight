package com.squareup.sqldelight.integration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.test.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.InterruptedException;
import java.util.List;
import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

public class IntegrationTests {
  private final DatabaseHelper helper = new DatabaseHelper(InstrumentationRegistry.getContext());
  private final SQLiteDatabase database = helper.getWritableDatabase();

  @Before public void before() {
    database.execSQL(Person.SEED_PEOPLE);
    database.execSQL(SqliteKeywords.SEED_SQLITE_KEYWORDS);
  }

  @After public void after() {
    database.execSQL(Person.DELETE_ALL);
    database.execSQL(Person.DELETE_ALL);
  }

  @Test public void indexedArgs() {
    // ?1 is the only arg
    SqlDelightStatement equivalentNames = Person.FACTORY.equivalent_names("Bob");
    Cursor cursor = database.rawQuery(equivalentNames.statement, equivalentNames.args);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_namesMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }

  @Test public void startIndexAtTwo() {
    // ?2 is the only arg
    SqlDelightStatement equivalentNames = Person.FACTORY.equivalent_names_2("Bob");
    Cursor cursor = database.rawQuery(equivalentNames.statement, equivalentNames.args);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_names_2Mapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }

  @Test public void namedIndexArgs() {
    // :name is the only arg
    SqlDelightStatement equivalentNames = Person.FACTORY.equivalent_names_named("Bob");
    Cursor cursor = database.rawQuery(equivalentNames.statement, equivalentNames.args);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_names_namedMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }

  @Test public void indexedArgLast() {
    // First arg declared is ?, second arg declared is ?1.
    SqlDelightStatement indexedArgLast = Person.FACTORY.indexed_arg_last("Bob");
    Cursor cursor = database.rawQuery(indexedArgLast.statement, indexedArgLast.args);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.indexed_arg_lastMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }

  @Test public void indexedArgLastTwo() {
    // First arg declared is ?, second arg declared is ?2.
    SqlDelightStatement indexedArgLast = Person.FACTORY.indexed_arg_last_2("Alec", "Strong");
    Cursor cursor = database.rawQuery(indexedArgLast.statement, indexedArgLast.args);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_namesMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(1, "Alec", "Strong"));
  }

  @Test public void nameIn() {
    SqlDelightStatement nameIn = Person.FACTORY.name_in(new String[] { "Alec", "Matt", "Jake" });
    Cursor cursor = database.rawQuery(nameIn.statement, nameIn.args);
    assertThat(cursor.getCount()).isEqualTo(3);
  }

  @Test public void sqliteKeywordQuery() {
    Cursor cursor = database.rawQuery(SqliteKeywords.SELECT_ALL, new String[0]);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    SqliteKeywords sqliteKeywords = SqliteKeywords.FACTORY.select_allMapper().map(cursor);
    assertThat(sqliteKeywords).isEqualTo(new AutoValue_SqliteKeywords(1, 10, 20));
  }

  @Test public void sqliteKeywordColumnString() {
    Cursor cursor = database.rawQuery(SqliteKeywords.SELECT_ALL, new String[0]);
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    long where = cursor.getLong(cursor.getColumnIndexOrThrow(SqliteKeywords.WHERE));
    assertThat(where).isEqualTo(10);
  }

  @Test public void compiledStatement() {
    SQLiteStatement statement = database.compileStatement(SqliteKeywords.INSERT_STMT);
    SqliteKeywords.FACTORY.insert_stmt(statement, 11, 21);
    statement.executeInsert();
    SqliteKeywords.FACTORY.insert_stmt(statement, 12, 22);
    statement.executeInsert();

    Cursor cursor = database.rawQuery(SqliteKeywords.SELECT_ALL, new String[0]);
    long current = 10;
    while (cursor.moveToNext()) {
      assertThat(cursor.getLong(cursor.getColumnIndexOrThrow(SqliteKeywords.WHERE))).isEqualTo(current++);
    }
  }

  @Test public void compiledStatementAcrossThread() {
    SQLiteStatement statement = database.compileStatement(SqliteKeywords.INSERT_STMT);
    SqliteKeywords.FACTORY.insert_stmt(statement, 11, 21);
    statement.executeInsert();

    new Thread(new Runnable() {
      @Override public void run() {
        synchronized (statement) {
          SqliteKeywords.FACTORY.insert_stmt(statement, 12, 22);
          statement.executeInsert();
          statement.notify();
        }
      }
    }).start();

    try {
      synchronized (statement) {
        statement.wait();
      }
    } catch (InterruptedException e) {

    }

    Cursor cursor = database.rawQuery(SqliteKeywords.SELECT_ALL, new String[0]);
    long current = 10;
    while (cursor.moveToNext()) {
      assertThat(cursor.getLong(cursor.getColumnIndexOrThrow(SqliteKeywords.WHERE))).isEqualTo(current++);
    }
  }
}
