package com.squareup.sqldelight.integration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    Cursor cursor = database.rawQuery(Person.EQUIVALENT_NAMES, new String[] { "Bob" });
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_namesMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }

  @Test public void startIndexAtTwo() {
    // ?2 is the only arg
    Cursor cursor = database.rawQuery(Person.EQUIVALENT_NAMES_2, new String[] { "ignored", "Bob" });
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_namesMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }

  @Test public void namedIndexArgs() {
    // :name is the only arg
    Cursor cursor = database.rawQuery(Person.EQUIVALENT_NAMES_NAMED, new String[] { "Bob" });
    assertThat(cursor.getCount()).isEqualTo(1);
    cursor.moveToFirst();
    Person person = Person.FACTORY.equivalent_namesMapper().map(cursor);
    assertThat(person).isEqualTo(new AutoValue_Person(4, "Bob", "Bob"));
  }
}
