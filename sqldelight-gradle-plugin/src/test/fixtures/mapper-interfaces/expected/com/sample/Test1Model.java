package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import com.test.Test2Model;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;

public interface Test1Model {
  String TABLE_NAME = "test1";

  String _ID = "_id";

  String DATE = "date";

  String CREATE_TABLE = ""
      + "CREATE TABLE test1 (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  date TEXT\n"
      + ")";

  @Nullable
  Long _id();

  @Nullable
  Date date();

  interface Join_tablesModel<T1 extends Test1Model, T2 extends Test2Model> {
    @NonNull
    T1 test1();

    @NonNull
    T2 test2();
  }

  interface Join_tablesCreator<T1 extends Test1Model, T2 extends Test2Model, T extends Join_tablesModel<T1, T2>> {
    T create(@NonNull T1 test1, @NonNull T2 test2);
  }

  final class Join_tablesMapper<T1 extends Test1Model, T2 extends Test2Model, T extends Join_tablesModel<T1, T2>> implements RowMapper<T> {
    private final Join_tablesCreator<T1, T2, T> creator;

    private final Factory<T1> test1ModelFactory;

    private final Test2Model.Factory<T2> test2ModelFactory;

    public Join_tablesMapper(Join_tablesCreator<T1, T2, T> creator, Factory<T1> test1ModelFactory,
        Test2Model.Factory<T2> test2ModelFactory) {
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
              cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.decode(cursor.getString(1))
          ),
          test2ModelFactory.creator.create(
              cursor.isNull(2) ? null : cursor.getLong(2)
          )
      );
    }
  }

  interface Creator<T extends Test1Model> {
    T create(@Nullable Long _id, @Nullable Date date);
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
          cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.decode(cursor.getString(1))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Date, String> dateAdapter;

    Marshal(@Nullable Test1Model copy, ColumnAdapter<Date, String> dateAdapter) {
      this.dateAdapter = dateAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.date(copy.date());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(Long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal date(@Nullable Date date) {
      if (date != null) {
        contentValues.put("date", dateAdapter.encode(date));
      } else {
        contentValues.putNull("date");
      }
      return this;
    }
  }

  final class Factory<T extends Test1Model> {
    public final Creator<T> creator;

    public final ColumnAdapter<Date, String> dateAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Date, String> dateAdapter) {
      this.creator = creator;
      this.dateAdapter = dateAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, dateAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(Test1Model copy) {
      return new Marshal(copy, dateAdapter);
    }

    public SqlDelightStatement join_tables() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test1\n"
          + "JOIN test2",
          new String[0], Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test1","test2"))));
    }

    public <T2 extends Test2Model, R extends Join_tablesModel<T, T2>> Join_tablesMapper<T, T2, R> join_tablesMapper(Join_tablesCreator<T, T2, R> creator,
        Test2Model.Factory<T2> test2ModelFactory) {
      return new Join_tablesMapper<T, T2, R>(creator, this, test2ModelFactory);
    }
  }
}
