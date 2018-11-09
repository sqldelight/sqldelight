package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long _id();

  interface Some_selectModel {
    long _id();

    long two__id();
  }

  interface Some_selectCreator<T extends Some_selectModel> {
    T create(long _id, long two__id);
  }

  final class Some_selectMapper<T extends Some_selectModel> implements RowMapper<T> {
    private final Some_selectCreator<T> creator;

    public Some_selectMapper(Some_selectCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getLong(1)
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long _id);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery some_select() {
      return new SqlDelightQuery(""
          + "SELECT one._id, two._id\n"
          + "FROM test one, test two",
          new TableSet("test"));
    }

    @NonNull
    public <R extends Some_selectModel> Some_selectMapper<R> some_selectMapper(
        Some_selectCreator<R> creator) {
      return new Some_selectMapper<R>(creator);
    }
  }
}
