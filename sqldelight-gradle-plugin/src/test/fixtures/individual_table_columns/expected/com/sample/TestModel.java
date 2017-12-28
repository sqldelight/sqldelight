package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String VIEW1_VIEW_NAME = "view1";

  String VIEW2_VIEW_NAME = "view2";

  String TABLE_NAME = "test";

  String _ID = "_id";

  String COLUMN1 = "column1";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  column1 TEXT\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW view1 AS\n"
      + "SELECT *\n"
      + "FROM test";

  String SOME_VIEW2 = ""
      + "CREATE VIEW view2 AS\n"
      + "SELECT _id, column1\n"
      + "FROM test";

  long _id();

  @Nullable
  String column1();

  interface Table_columns_selectModel {
    long _id();

    @Nullable
    String column1();
  }

  interface Table_columns_selectCreator<T extends Table_columns_selectModel> {
    T create(long _id, @Nullable String column1);
  }

  final class Table_columns_selectMapper<T extends Table_columns_selectModel> implements RowMapper<T> {
    private final Table_columns_selectCreator<T> creator;

    public Table_columns_selectMapper(Table_columns_selectCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface View_columns_selectModel {
    long _id();

    @Nullable
    String column1();
  }

  interface View_columns_selectCreator<T extends View_columns_selectModel> {
    T create(long _id, @Nullable String column1);
  }

  final class View_columns_selectMapper<T extends View_columns_selectModel> implements RowMapper<T> {
    private final View_columns_selectCreator<T> creator;

    public View_columns_selectMapper(View_columns_selectCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Column_view_column_selectModel {
    long _id();

    @Nullable
    String column1();
  }

  interface Column_view_column_selectCreator<T extends Column_view_column_selectModel> {
    T create(long _id, @Nullable String column1);
  }

  final class Column_view_column_selectMapper<T extends Column_view_column_selectModel> implements RowMapper<T> {
    private final Column_view_column_selectCreator<T> creator;

    public Column_view_column_selectMapper(Column_view_column_selectCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface View1Model<T1 extends TestModel> {
    @NonNull
    T1 test();
  }

  interface View1Creator<T1 extends TestModel, T extends View1Model<T1>> {
    T create(@NonNull T1 test);
  }

  interface View2Model {
    long _id();

    @Nullable
    String column1();
  }

  interface View2Creator<T extends View2Model> {
    T create(long _id, @Nullable String column1);
  }

  final class View1Mapper<T1 extends TestModel, T extends View1Model<T1>> implements RowMapper<T> {
    private final View1Creator<T1, T> creator;

    private final Factory<T1> testModelFactory;

    public View1Mapper(View1Creator<T1, T> creator, Factory<T1> testModelFactory) {
      this.creator = creator;
      this.testModelFactory = testModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          testModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getString(1)
          )
      );
    }
  }

  final class View2Mapper<T extends View2Model> implements RowMapper<T> {
    private final View2Creator<T> creator;

    public View2Mapper(View2Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long _id, @Nullable String column1);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightQuery table_select() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM test",
          new TableSet("test"));
    }

    public SqlDelightQuery table_columns_select() {
      return new SqlDelightQuery(""
          + "SELECT _id, column1\n"
          + "FROM test",
          new TableSet("test"));
    }

    public SqlDelightQuery view_select() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM view1",
          new TableSet("test"));
    }

    public SqlDelightQuery view_columns_select() {
      return new SqlDelightQuery(""
          + "SELECT _id, column1\n"
          + "FROM view1",
          new TableSet("test"));
    }

    public SqlDelightQuery column_view_select() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM view2",
          new TableSet("test"));
    }

    public SqlDelightQuery column_view_column_select() {
      return new SqlDelightQuery(""
          + "SELECT _id, column1\n"
          + "FROM view2",
          new TableSet("test"));
    }

    public Mapper<T> table_selectMapper() {
      return new Mapper<T>(this);
    }

    public <R extends Table_columns_selectModel> Table_columns_selectMapper<R> table_columns_selectMapper(Table_columns_selectCreator<R> creator) {
      return new Table_columns_selectMapper<R>(creator);
    }

    public <R extends View1Model<T>> View1Mapper<T, R> view_selectMapper(View1Creator<T, R> creator) {
      return new View1Mapper<T, R>(creator, this);
    }

    public <R extends View_columns_selectModel> View_columns_selectMapper<R> view_columns_selectMapper(View_columns_selectCreator<R> creator) {
      return new View_columns_selectMapper<R>(creator);
    }

    public <R extends View2Model> View2Mapper<R> column_view_selectMapper(View2Creator<R> creator) {
      return new View2Mapper<R>(creator);
    }

    public <R extends Column_view_column_selectModel> Column_view_column_selectMapper<R> column_view_column_selectMapper(Column_view_column_selectCreator<R> creator) {
      return new Column_view_column_selectMapper<R>(creator);
    }
  }
}
