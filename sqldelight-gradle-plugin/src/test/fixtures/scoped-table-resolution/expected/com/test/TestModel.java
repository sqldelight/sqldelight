package com.test;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String SOME_VIEW_VIEW_NAME = "some_view";

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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement some_select() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test\n"
          + "WHERE _id IN some_view",
          new Object[0], new TableSet("test"));
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }
  }
}
