package com.squareup.sqldelight.integration;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.support.test.InstrumentationRegistry;
import com.squareup.sqldelight.android.SqlDelightDatabaseHelper;
import com.squareup.sqldelight.android.SqlDelight;
import com.squareup.sqldelight.db.SqlDatabase;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;
import java.io.File;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class MigrationTest {
  @Test public void testMigrationWorks() {
    // Set up version 1 db.
    SupportSQLiteOpenHelper.Callback callback = new SupportSQLiteOpenHelper.Callback(1) {
      @Override public void onCreate(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE person (\n"
            + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
            + "  first_name TEXT NOT NULL\n"
            + ");");
        db.execSQL("CREATE TABLE `group` (\n"
            + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
            + "  'where' INTEGER NOT NULL,\n"
            + "  [having] INTEGER NOT NULL\n"
            + ");");
        db.execSQL("INSERT INTO person VALUES (1, 'alec');");
      }

      @Override public void onUpgrade(
          SupportSQLiteDatabase db,
          int oldVersion,
          int newVersion
      ) {
      }
    };
    SupportSQLiteOpenHelper.Configuration configuration = SupportSQLiteOpenHelper.Configuration
        .builder(InstrumentationRegistry.getTargetContext())
        .callback(callback)
        .name("test.db")
        .build();
    SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(configuration);
    // Creates the db.
    helper.getWritableDatabase().execSQL("DELETE FROM person WHERE first_name NOT IN ('alec')");
    helper.close();

    // Migrate the db with a queryWrapper
    SqlDatabase database = SqlDelight.create(QueryWrapper.Companion,
        InstrumentationRegistry.getTargetContext(), "test.db");
    QueryWrapper queryWrapper = new QueryWrapper(database);

    // Assert info is correct
    Person person = queryWrapper.getPersonQueries()
        .nameIn(Arrays.asList("alec"), AutoValue_MyPerson::new).executeAsOne();
    assertThat(person).isEqualTo(new AutoValue_MyPerson(1, "alec", "sup"));
    database.close();
  }
}
