package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long _id();

  interface Select_stuffModel {
    long _id();

    @NonNull
    String test__id();
  }

  interface Select_stuffCreator<T extends Select_stuffModel> {
    T create(long _id, @NonNull String test__id);
  }

  final class Select_stuffMapper<T extends Select_stuffModel> implements RowMapper<T> {
    private final Select_stuffCreator<T> creator;

    public Select_stuffMapper(Select_stuffCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getString(1)
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long _id);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement select_stuff() {
      return new SqlDelightStatement(""
          + "SELECT _id, CAST (_id AS TEXT)\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    public <R extends Select_stuffModel> Select_stuffMapper<R> select_stuffMapper(Select_stuffCreator<R> creator) {
      return new Select_stuffMapper<R>(creator);
    }
  }
}
