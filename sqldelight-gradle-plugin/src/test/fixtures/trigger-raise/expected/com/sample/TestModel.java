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

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  String SOME_TRIGGER = ""
      + "CREATE TRIGGER subject_id_changes\n"
      + "    BEFORE UPDATE OF _id ON test WHEN old._id != new._id\n"
      + "    BEGIN SELECT raise(fail, 'Not allowed to change column id in subject'); END";

  long _id();

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(_ID))
      );
    }

    public interface Creator<R extends TestModel> {
      R create(long _id);
    }
  }

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public Marshal() {
    }

    public Marshal(TestModel copy) {
      this._id(copy._id());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }
  }
}
