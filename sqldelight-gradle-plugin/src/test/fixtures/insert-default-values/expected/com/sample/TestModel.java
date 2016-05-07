package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  name TEXT NOT NULL DEFAULT 'alec'\n"
      + ")";

  String INSERT_DEFAULT_VALUES = ""
      + "INSERT INTO test DEFAULT VALUES";

  long _id();

  @NonNull
  String name();

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(_ID)),
          cursor.getString(cursor.getColumnIndex(NAME))
      );
    }

    public interface Creator<R extends TestModel> {
      R create(long _id, String name);
    }
  }

  class TestMarshal<T extends TestMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public TestMarshal() {
    }

    public TestMarshal(TestModel copy) {
      this._id(copy._id());
      this.name(copy.name());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T name(String name) {
      contentValues.put(NAME, name);
      return (T) this;
    }
  }
}
