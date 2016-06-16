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

  interface Join_tablesModel<T1 extends Test1Model, T3 extends Test2Model> {
    T1 test1();

    T3 test2();
  }

  interface Join_tablesCreator<T1 extends Test1Model, T3 extends Test2Model, T extends Join_tablesModel<T1, T3>> {
    T create(T1 test1, T3 test2);
  }

  final class Join_tablesMapper<T1 extends Test1Model, T3 extends Test2Model, T extends Join_tablesModel<T1, T3>> implements RowMapper<T> {
    private final Join_tablesCreator<T1, T3, T> creator;

    private final Factory<T1> test1ModelFactory;

    private final Test2Model.Factory<T3> test2ModelFactory;

    private Join_tablesMapper(Join_tablesCreator<T1, T3, T> creator, Factory<T1> test1ModelFactory, Test2Model.Factory<T3> test2ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.map(cursor, 1)
          ),
          test2ModelFactory.creator.create(
              cursor.isNull(2) ? null : cursor.getLong(2)
          )
      );
    }
  }

  interface Creator<T extends Test1Model> {
    T create(Long _id, Date date);
  }

  final class Mapper<T extends Test1Model> implements RowMapper<T> {
    private final Factory<T> test1ModelFactory;

    public Mapper(Factory<T> test1ModelFactory) {
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test1ModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.map(cursor, 1)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Date> dateAdapter;

    Marshal(ColumnAdapter<Date> dateAdapter) {
      this.dateAdapter = dateAdapter;
    }

    Marshal(Test1Model copy, ColumnAdapter<Date> dateAdapter) {
      this._id(copy._id());
      this.dateAdapter = dateAdapter;
      this.date(copy.date());
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(Long _id) {
      contentValues.put(_ID, _id);
      return this;
    }

    public Marshal date(Date date) {
      dateAdapter.marshal(contentValues, DATE, date);
      return this;
    }
  }

  final class Factory<T extends Test1Model> {
    public final Creator<T> creator;

    public final ColumnAdapter<Date> dateAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Date> dateAdapter) {
      this.creator = creator;
      this.dateAdapter = dateAdapter;
    }

    public Marshal marshal() {
      return new Marshal(dateAdapter);
    }

    public Marshal marshal(Test1Model copy) {
      return new Marshal(copy, dateAdapter);
    }

    public <T3 extends Test2Model, R extends Join_tablesModel<T, T3>> Join_tablesMapper<T, T3, R> join_tablesMapper(Join_tablesCreator<T, T3, R> creator, Test2Model.Factory<T3> test2ModelFactory) {
      return new Join_tablesMapper<T, T3, R>(creator, this, test2ModelFactory);
    }
  }
}
