package com.test;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String SOME_TEXT = "some_text";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  some_text TEXT NOT NULL\n"
      + ")";

  @Nullable
  Long _id();

  @NonNull
  String some_text();

  interface Creator<T extends TestModel> {
    T create(@Nullable Long _id, @NonNull String some_text);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getString(1)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement some_select() {
      return new SqlDelightStatement(""
          + "SELECT CASE _id WHEN 0 THEN some_text ELSE some_text + _id END AS indexed_text\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    public RowMapper<String> some_selectMapper() {
      return new RowMapper<String>() {
        @Override
        public String map(Cursor cursor) {
          return cursor.getString(0);
        }
      };
    }
  }
}
