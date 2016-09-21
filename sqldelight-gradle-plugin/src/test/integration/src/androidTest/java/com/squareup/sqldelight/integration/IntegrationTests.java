package com.squareup.sqldelight.integration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.squareup.sqldelight.SqlDelightStatement;

import static com.google.common.truth.Truth.assertThat;

public class IntegrationTests {
  private final DatabaseHelper helper = new DatabaseHelper(InstrumentationRegistry.getContext());
  private final SQLiteDatabase database = helper.getWritableDatabase();

  @Before public void before() {
    database.execSQL(Person.SEED_PEOPLE);
  }

  @After public void after() {
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
}
