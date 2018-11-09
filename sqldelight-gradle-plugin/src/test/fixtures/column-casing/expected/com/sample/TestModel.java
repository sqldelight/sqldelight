package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String STUFF = "STUFF";

  @Deprecated
  String MYSTUFF = "mySTUFF";

  @Deprecated
  String LOWERCASE_STUFF = "lowercase_stuff";

  @Deprecated
  String MYOTHERSTUFF = "myOtherStuff";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  STUFF TEXT NOT NULL,\n"
      + "  mySTUFF TEXT NOT NULL,\n"
      + "  lowercase_stuff TEXT NOT NULL,\n"
      + "  myOtherStuff TEXT NOT NULL\n"
      + ")";

  @NonNull
  String STUFF();

  @NonNull
  String mySTUFF();

  @NonNull
  String lowercase_stuff();

  @NonNull
  String myOtherStuff();

  interface Some_selectModel {
    @NonNull
    String mySTUFF();

    @NonNull
    String myOtherStuff();
  }

  interface Some_selectCreator<T extends Some_selectModel> {
    T create(@NonNull String mySTUFF, @NonNull String myOtherStuff);
  }

  final class Some_selectMapper<T extends Some_selectModel> implements RowMapper<T> {
    private final Some_selectCreator<T> creator;

    public Some_selectMapper(Some_selectCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.getString(1)
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(@NonNull String STUFF, @NonNull String mySTUFF, @NonNull String lowercase_stuff,
        @NonNull String myOtherStuff);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getString(0),
          cursor.getString(1),
          cursor.getString(2),
          cursor.getString(3)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery some_select() {
      return new SqlDelightQuery(""
          + "SELECT mySTUFF, myOtherStuff\n"
          + "FROM test",
          new TableSet("test"));
    }

    @NonNull
    public <R extends Some_selectModel> Some_selectMapper<R> some_selectMapper(
        Some_selectCreator<R> creator) {
      return new Some_selectMapper<R>(creator);
    }
  }
}
