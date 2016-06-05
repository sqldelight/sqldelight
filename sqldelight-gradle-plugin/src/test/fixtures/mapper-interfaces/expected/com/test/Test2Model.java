package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.sample.Test1Model;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface Test2Model {
  String TABLE_NAME = "test2";

  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test2 (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  String JOIN_TABLES = ""
      + "SELECT *\n"
      + "FROM test2\n"
      + "JOIN test1";

  @Nullable
  Long _id();

  interface Join_tablesModel {
    Test2Model test2();

    Test1Model test1();
  }

  final class Mapper<T extends Test2Model> implements RowMapper<T> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(_ID)) ? null : cursor.getLong(cursor.getColumnIndex(_ID))
      );
    }

    public interface Creator<R extends Test2Model> {
      R create(Long _id);
    }
  }

  class Test2Marshal<T extends Test2Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public Test2Marshal() {
    }

    public Test2Marshal(Test2Model copy) {
      this._id(copy._id());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(Long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }
  }
}
