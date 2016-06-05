package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.test.Test2Model;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Date;

public interface Test1Model {
  String TABLE_NAME = "test1";

  String _ID = "_id";

  String DATE = "date";

  String CREATE_TABLE = ""
      + "CREATE TABLE test1 (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  date TEXT\n"
      + ")";

  String JOIN_TABLES = ""
      + "SELECT *\n"
      + "FROM test1\n"
      + "JOIN test2";

  @Nullable
  Long _id();

  @Nullable
  Date date();

  interface Join_tablesModel {
    Test1Model test1();

    Test2Model test2();
  }

  final class Mapper<T extends Test1Model> implements RowMapper<T> {
    private final Creator<T> creator;

    private final ColumnAdapter<Date> dateAdapter;

    protected Mapper(Creator<T> creator, ColumnAdapter<Date> dateAdapter) {
      this.creator = creator;
      this.dateAdapter = dateAdapter;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(_ID)) ? null : cursor.getLong(cursor.getColumnIndex(_ID)),
          cursor.isNull(cursor.getColumnIndex(DATE)) ? null : dateAdapter.map(cursor, cursor.getColumnIndex(DATE))
      );
    }

    public interface Creator<R extends Test1Model> {
      R create(Long _id, Date date);
    }
  }

  class Test1Marshal<T extends Test1Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Date> dateAdapter;

    public Test1Marshal(ColumnAdapter<Date> dateAdapter) {
      this.dateAdapter = dateAdapter;
    }

    public Test1Marshal(Test1Model copy, ColumnAdapter<Date> dateAdapter) {
      this._id(copy._id());
      this.dateAdapter = dateAdapter;
      this.date(copy.date());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(Long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T date(Date date) {
      dateAdapter.marshal(contentValues, DATE, date);
      return (T) this;
    }
  }
}
