package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Collections;

public interface TestModel {
  String SOME_VIEW_VIEW_NAME = "some_view";

  String TABLE_NAME = "settings";

  String ROW_ID = "row_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE settings (\n"
      + "  row_id INTEGER NOT NULL PRIMARY KEY\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW some_view AS\n"
      + "SELECT B.row_id\n"
      + "FROM settings\n"
      + "LEFT JOIN settings B USING (row_id)";

  long row_id();

  interface Some_viewModel {
    @Nullable
    Long row_id();
  }

  interface Some_viewCreator<T extends Some_viewModel> {
    T create(@Nullable Long row_id);
  }

  final class Some_viewMapper<T extends Some_viewModel> implements RowMapper<T> {
    private final Some_viewCreator<T> creator;

    public Some_viewMapper(Some_viewCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0)
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long row_id);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement some_select(@Nullable Long row_id) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM some_view\n"
              + "WHERE row_id IN (SELECT B.row_id FROM some_view B WHERE B.row_id = ");
      if (row_id == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append(row_id);
      }
      query.append(")");
      return new SqlDelightStatement(query.toString(), new String[0], Collections.<String>singleton("settings"));
    }

    public <R extends Some_viewModel> Some_viewMapper<R> some_selectMapper(Some_viewCreator<R> creator) {
      return new Some_viewMapper<R>(creator);
    }
  }
}
