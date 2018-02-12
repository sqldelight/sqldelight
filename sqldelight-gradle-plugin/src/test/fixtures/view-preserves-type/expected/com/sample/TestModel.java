package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String VIEW1_VIEW_NAME = "view1";

  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String A_BOOLEAN = "a_boolean";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  a_boolean INTEGER NOT NULL\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW view1 AS\n"
      + "SELECT a_boolean AS one, 1 AS two\n"
      + "FROM test\n"
      + "UNION\n"
      + "SELECT 0, a_boolean\n"
      + "FROM test";

  boolean a_boolean();

  interface Some_selectModel {
    boolean one();

    boolean int_literal();
  }

  interface Some_selectCreator<T extends Some_selectModel> {
    T create(boolean one, boolean int_literal);
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
          cursor.getInt(0) == 1,
          cursor.getInt(1) == 1
      );
    }
  }

  interface View1Model {
    boolean one();

    boolean two();
  }

  interface View1Creator<T extends View1Model> {
    T create(boolean one, boolean two);
  }

  final class View1Mapper<T extends View1Model> implements RowMapper<T> {
    private final View1Creator<T> creator;

    public View1Mapper(View1Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getInt(0) == 1,
          cursor.getInt(1) == 1
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(boolean a_boolean);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getInt(0) == 1
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
          + "SELECT one, 1\n"
          + "FROM view1\n"
          + "UNION ALL\n"
          + "SELECT 0, a_boolean\n"
          + "FROM test",
          new TableSet("test"));
    }

    @NonNull
    public SqlDelightQuery select_from_view() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM view1",
          new TableSet("test"));
    }

    @NonNull
    public <R extends Some_selectModel> Some_selectMapper<R> some_selectMapper(
        Some_selectCreator<R> creator) {
      return new Some_selectMapper<R>(creator);
    }

    @NonNull
    public <R extends View1Model> View1Mapper<R> select_from_viewMapper(View1Creator<R> creator) {
      return new View1Mapper<R>(creator);
    }
  }
}
