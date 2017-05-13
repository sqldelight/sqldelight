package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

public interface Test2Model {
  String SOME_VIEW_VIEW_NAME = "some_view";

  String TABLE_NAME = "test2";

  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test2 (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW some_view AS\n"
      + "SELECT test1._id\n"
      + "FROM test1\n"
      + "JOIN test2 USING (_id)";

  long _id();

  interface Some_viewModel {
    long _id();
  }

  interface Some_viewCreator<T extends Some_viewModel> {
    T create(long _id);
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
          cursor.getLong(0)
      );
    }
  }

  interface Creator<T extends Test2Model> {
    T create(long _id);
  }

  final class Mapper<T extends Test2Model> implements RowMapper<T> {
    private final Factory<T> test2ModelFactory;

    public Mapper(Factory<T> test2ModelFactory) {
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test2ModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Factory<T extends Test2Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement query_with_arg(long _id) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM some_view\n"
              + "WHERE _id=");
      query.append(_id);
      return new SqlDelightStatement(query.toString(), new String[0], Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test1","test2"))));
    }

    public <R extends Some_viewModel> Some_viewMapper<R> query_with_argMapper(Some_viewCreator<R> creator) {
      return new Some_viewMapper<R>(creator);
    }
  }
}
