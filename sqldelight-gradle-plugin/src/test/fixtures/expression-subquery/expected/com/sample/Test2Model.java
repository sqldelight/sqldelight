package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface Test2Model {
  @Deprecated
  String TABLE_NAME = "test2";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String TESTID = "testId";

  String CREATE_TABLE = ""
      + "CREATE TABLE test2 (\n"
      + "  _id\tINTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  testId\tINTEGER NOT NULL\n"
      + ")";

  long _id();

  long testId();

  interface Creator<T extends Test2Model> {
    T create(long _id, long testId);
  }

  final class Mapper<T extends Test2Model> implements RowMapper<T> {
    private final Factory<T> test2ModelFactory;

    public Mapper(@NonNull Factory<T> test2ModelFactory) {
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test2ModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1)
      );
    }
  }

  final class Factory<T extends Test2Model> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }
}
