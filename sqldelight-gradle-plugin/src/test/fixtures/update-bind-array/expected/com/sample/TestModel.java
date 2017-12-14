package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String SOME_TEXT = "some_text";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  some_text TEXT NOT NULL\n"
      + ")";

  long _id();

  @NonNull
  String some_text();

  interface Creator<T extends TestModel> {
    T create(long _id, @NonNull String some_text);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement some_update(long[] _id) {
      StringBuilder query = new StringBuilder();
      query.append("UPDATE test\n"
              + "SET some_text = 'test'\n"
              + "WHERE _id IN ");
      query.append('(');
      for (int i = 0; i < _id.length; i++) {
        if (i != 0) query.append(", ");
        query.append(_id[i]);
      }
      query.append(')');
      return new SqlDelightStatement(query.toString(), new String[0], new TableSet("test"));
    }
  }
}
