package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW some_view AS\n"
      + "SELECT _id, count(*)\n"
      + "FROM test\n"
      + "GROUP BY _id";

  String SOME_SELECT = ""
      + "SELECT *\n"
      + "FROM test\n"
      + "WHERE _id IN some_view";

  @Nullable
  Long _id();

  interface Some_viewModel {
    @Nullable
    Long _id();

    long count();
  }

  interface Some_viewCreator<T extends Some_viewModel> {
    T create(@Nullable Long _id, long count);
  }

  interface Creator<T extends TestModel> {
    T create(@Nullable Long _id);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(Long _id) {
      contentValues.put("_id", _id);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
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
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }
  }
}
