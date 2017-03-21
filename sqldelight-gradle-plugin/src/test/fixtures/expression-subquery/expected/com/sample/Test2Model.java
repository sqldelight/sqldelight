package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface Test2Model {
  String TABLE_NAME = "test2";

  String _ID = "_id";

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

    public Mapper(Factory<T> test2ModelFactory) {
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable Test2Model copy) {
      if (copy != null) {
        this._id(copy._id());
        this.testId(copy.testId());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal testId(long testId) {
      contentValues.put("testId", testId);
      return this;
    }
  }

  final class Factory<T extends Test2Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(Test2Model copy) {
      return new Marshal(copy);
    }
  }
}
