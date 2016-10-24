package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public interface Test2Model {
  String TABLE_NAME = "test2";

  String _ID = "_id";

  String COLUMN1 = "column1";

  String COLUMN2 = "column2";

  String CREATE_TABLE = ""
      + "CREATE TABLE test2 (\n"
      + "    _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "    column1 TEXT NOT NULL,\n"
      + "    column2 TEXT NOT NULL\n"
      + ")";

  String OTHER_SELECT = ""
      + "SELECT *\n"
      + "FROM test2\n"
      + "JOIN view1 USING (_id)";

  String VIEW_SELECT = ""
      + "SELECT *\n"
      + "FROM view1";

  String VIEW_USING_TABLE = ""
      + "CREATE VIEW test2_copy AS\n"
      + "SELECT *\n"
      + "FROM test2";

  String COPY_VIEW_SELECT = ""
      + "SELECT *\n"
      + "FROM test2_copy";

  String VIEW_USING_TABLES = ""
      + "CREATE VIEW multiple_tables AS\n"
      + "SELECT *\n"
      + "FROM test\n"
      + "JOIN test2 USING (_id)";

  String MULTIPLE_VIEW_SELECT = ""
      + "SELECT *\n"
      + "FROM test2_copy\n"
      + "JOIN multiple_tables";

  String VIEWS_AND_COLUMNS_SELECT = ""
      + "SELECT first_view.*, 'sup', second_view.*\n"
      + "FROM view1 first_view\n"
      + "JOIN view1 second_view";

  String SUB_VIEW = ""
      + "CREATE VIEW sub_view AS\n"
      + "SELECT first_view.*, 'sup', second_view.*\n"
      + "FROM view1 first_view\n"
      + "JOIN view1 second_view";

  String SELECT_FROM_SUB_VIEW = ""
      + "SELECT *, 'supsupsup'\n"
      + "FROM sub_view\n"
      + "JOIN test2_copy";

  String PROJECTION_VIEW = ""
      + "CREATE VIEW projection_view AS\n"
      + "SELECT column2 AS projection, column2 AS project_copy\n"
      + "FROM test";

  String SELECT_FROM_PROJECTION = ""
      + "SELECT *\n"
      + "FROM projection_view";

  String TEST2_PROJECTION = ""
      + "CREATE VIEW test2_projection AS\n"
      + "SELECT column2 AS project\n"
      + "FROM test2";

  String SELECT_FROM_TEST2_PROJECTION = ""
      + "SELECT *\n"
      + "FROM test2_projection";

  @Nullable
  Long _id();

  @NonNull
  String column1();

  @NonNull
  List column2();

  interface Other_selectModel<T1 extends Test2Model, V2 extends Test1Model.View1Model> {
    @NonNull
    T1 test2();

    @NonNull
    V2 view1();
  }

  interface Other_selectCreator<T1 extends Test2Model, V2 extends Test1Model.View1Model, T extends Other_selectModel<T1, V2>> {
    T create(@NonNull T1 test2, @NonNull V2 view1);
  }

  final class Other_selectMapper<T1 extends Test2Model, V2 extends Test1Model.View1Model, T extends Other_selectModel<T1, V2>> implements RowMapper<T> {
    private final Other_selectCreator<T1, V2, T> creator;

    private final Factory<T1> test2ModelFactory;

    private final Test1Model.View1Creator<V2> view1Creator;

    public Other_selectMapper(Other_selectCreator<T1, V2, T> creator, Factory<T1> test2ModelFactory, Test1Model.View1Creator<V2> view1Creator) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.view1Creator = view1Creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.getString(1),
              test2ModelFactory.column2Adapter.decode(cursor.getString(2))
          ),
          view1Creator.create(
              cursor.isNull(3) ? null : cursor.getLong(3),
              cursor.isNull(4) ? null : cursor.getLong(4)
          )
      );
    }
  }

  interface Multiple_view_selectModel<V1T1 extends Test2Model, V1 extends Test2_copyModel<V1T1>, V2T1 extends Test1Model, V2 extends Multiple_tablesModel<V2T1, V1T1>> {
    @NonNull
    V1 test2_copy();

    @NonNull
    V2 multiple_tables();
  }

  interface Multiple_view_selectCreator<V1T1 extends Test2Model, V1 extends Test2_copyModel<V1T1>, V2T1 extends Test1Model, V2 extends Multiple_tablesModel<V2T1, V1T1>, T extends Multiple_view_selectModel<V1T1, V1, V2T1, V2>> {
    T create(@NonNull V1 test2_copy, @NonNull V2 multiple_tables);
  }

  final class Multiple_view_selectMapper<V1T1 extends Test2Model, V1 extends Test2_copyModel<V1T1>, V2T1 extends Test1Model, V2 extends Multiple_tablesModel<V2T1, V1T1>, T extends Multiple_view_selectModel<V1T1, V1, V2T1, V2>> implements RowMapper<T> {
    private final Multiple_view_selectCreator<V1T1, V1, V2T1, V2, T> creator;

    private final Factory<V1T1> test2ModelFactory;

    private final Test2_copyCreator<V1T1, V1> test2_copyCreator;

    private final Test1Model.Factory<V2T1> test1ModelFactory;

    private final Multiple_tablesCreator<V2T1, V1T1, V2> multiple_tablesCreator;

    public Multiple_view_selectMapper(Multiple_view_selectCreator<V1T1, V1, V2T1, V2, T> creator, Factory<V1T1> test2ModelFactory, Test2_copyCreator<V1T1, V1> test2_copyCreator, Test1Model.Factory<V2T1> test1ModelFactory, Multiple_tablesCreator<V2T1, V1T1, V2> multiple_tablesCreator) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.test2_copyCreator = test2_copyCreator;
      this.test1ModelFactory = test1ModelFactory;
      this.multiple_tablesCreator = multiple_tablesCreator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2_copyCreator.create(
              test2ModelFactory.creator.create(
                  cursor.isNull(0) ? null : cursor.getLong(0),
                  cursor.getString(1),
                  test2ModelFactory.column2Adapter.decode(cursor.getString(2))
              )
          ),
          multiple_tablesCreator.create(
              test1ModelFactory.creator.create(
                  cursor.isNull(3) ? null : cursor.getLong(3),
                  cursor.isNull(4) ? null : cursor.getString(4),
                  cursor.isNull(5) ? null : test1ModelFactory.column2Adapter.decode(cursor.getLong(5))
              ),
              test2ModelFactory.creator.create(
                  cursor.isNull(6) ? null : cursor.getLong(6),
                  cursor.getString(7),
                  test2ModelFactory.column2Adapter.decode(cursor.getString(8))
              )
          )
      );
    }
  }

  interface Views_and_columns_selectModel<V1 extends Test1Model.View1Model> {
    @NonNull
    V1 first_view();

    @NonNull
    String string_literal();

    @NonNull
    V1 second_view();
  }

  interface Views_and_columns_selectCreator<V1 extends Test1Model.View1Model, T extends Views_and_columns_selectModel<V1>> {
    T create(@NonNull V1 first_view, @NonNull String string_literal, @NonNull V1 second_view);
  }

  final class Views_and_columns_selectMapper<V1 extends Test1Model.View1Model, T extends Views_and_columns_selectModel<V1>> implements RowMapper<T> {
    private final Views_and_columns_selectCreator<V1, T> creator;

    private final Test1Model.View1Creator<V1> view1Creator;

    public Views_and_columns_selectMapper(Views_and_columns_selectCreator<V1, T> creator, Test1Model.View1Creator<V1> view1Creator) {
      this.creator = creator;
      this.view1Creator = view1Creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          view1Creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getLong(1)
          ),
          cursor.getString(2),
          view1Creator.create(
              cursor.isNull(3) ? null : cursor.getLong(3),
              cursor.isNull(4) ? null : cursor.getLong(4)
          )
      );
    }
  }

  interface Select_from_sub_viewModel<V1V1 extends Test1Model.View1Model, V1 extends Sub_viewModel<V1V1>, V2T1 extends Test2Model, V2 extends Test2_copyModel<V2T1>> {
    @NonNull
    V1 sub_view();

    @NonNull
    V2 test2_copy();

    @NonNull
    String string_literal();
  }

  interface Select_from_sub_viewCreator<V1V1 extends Test1Model.View1Model, V1 extends Sub_viewModel<V1V1>, V2T1 extends Test2Model, V2 extends Test2_copyModel<V2T1>, T extends Select_from_sub_viewModel<V1V1, V1, V2T1, V2>> {
    T create(@NonNull V1 sub_view, @NonNull V2 test2_copy, @NonNull String string_literal);
  }

  final class Select_from_sub_viewMapper<V1V1 extends Test1Model.View1Model, V1 extends Sub_viewModel<V1V1>, V2T1 extends Test2Model, V2 extends Test2_copyModel<V2T1>, T extends Select_from_sub_viewModel<V1V1, V1, V2T1, V2>> implements RowMapper<T> {
    private final Select_from_sub_viewCreator<V1V1, V1, V2T1, V2, T> creator;

    private final Sub_viewCreator<V1V1, V1> sub_viewCreator;

    private final Test1Model.View1Creator<V1V1> view1Creator;

    private final Factory<V2T1> test2ModelFactory;

    private final Test2_copyCreator<V2T1, V2> test2_copyCreator;

    public Select_from_sub_viewMapper(Select_from_sub_viewCreator<V1V1, V1, V2T1, V2, T> creator, Sub_viewCreator<V1V1, V1> sub_viewCreator, Test1Model.View1Creator<V1V1> view1Creator, Factory<V2T1> test2ModelFactory, Test2_copyCreator<V2T1, V2> test2_copyCreator) {
      this.creator = creator;
      this.sub_viewCreator = sub_viewCreator;
      this.view1Creator = view1Creator;
      this.test2ModelFactory = test2ModelFactory;
      this.test2_copyCreator = test2_copyCreator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          sub_viewCreator.create(
              view1Creator.create(
                  cursor.isNull(0) ? null : cursor.getLong(0),
                  cursor.isNull(1) ? null : cursor.getLong(1)
              ),
              cursor.getString(2),
              view1Creator.create(
                  cursor.isNull(3) ? null : cursor.getLong(3),
                  cursor.isNull(4) ? null : cursor.getLong(4)
              )
          ),
          test2_copyCreator.create(
              test2ModelFactory.creator.create(
                  cursor.isNull(5) ? null : cursor.getLong(5),
                  cursor.getString(6),
                  test2ModelFactory.column2Adapter.decode(cursor.getString(7))
              )
          ),
          cursor.getString(8)
      );
    }
  }

  interface Test2_copyModel<T1 extends Test2Model> {
    @NonNull
    T1 test2();
  }

  interface Test2_copyCreator<T1 extends Test2Model, T extends Test2_copyModel<T1>> {
    T create(@NonNull T1 test2);
  }

  interface Multiple_tablesModel<T1 extends Test1Model, T2 extends Test2Model> {
    @NonNull
    T1 test();

    @NonNull
    T2 test2();
  }

  interface Multiple_tablesCreator<T1 extends Test1Model, T2 extends Test2Model, T extends Multiple_tablesModel<T1, T2>> {
    T create(@NonNull T1 test, @NonNull T2 test2);
  }

  interface Sub_viewModel<V1 extends Test1Model.View1Model> {
    @NonNull
    V1 first_view();

    @NonNull
    String string_literal();

    @NonNull
    V1 second_view();
  }

  interface Sub_viewCreator<V1 extends Test1Model.View1Model, T extends Sub_viewModel<V1>> {
    T create(@NonNull V1 first_view, @NonNull String string_literal, @NonNull V1 second_view);
  }

  interface Projection_viewModel {
    @Nullable
    List projection();

    @Nullable
    List project_copy();
  }

  interface Projection_viewCreator<T extends Projection_viewModel> {
    T create(@Nullable List projection, @Nullable List project_copy);
  }

  interface Test2_projectionModel {
    @NonNull
    List project();
  }

  interface Test2_projectionCreator<T extends Test2_projectionModel> {
    T create(@NonNull List project);
  }

  final class View1Mapper<T extends Test1Model.View1Model> implements RowMapper<T> {
    private final Test1Model.View1Creator<T> creator;

    public View1Mapper(Test1Model.View1Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getLong(1)
      );
    }
  }

  final class Test2_copyMapper<T1 extends Test2Model, T extends Test2_copyModel<T1>> implements RowMapper<T> {
    private final Test2_copyCreator<T1, T> creator;

    private final Factory<T1> test2ModelFactory;

    public Test2_copyMapper(Test2_copyCreator<T1, T> creator, Factory<T1> test2ModelFactory) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.getString(1),
              test2ModelFactory.column2Adapter.decode(cursor.getString(2))
          )
      );
    }
  }

  final class Projection_viewMapper<T extends Projection_viewModel, T1 extends Test1Model> implements RowMapper<T> {
    private final Projection_viewCreator<T> creator;

    private final Test1Model.Factory<T1> test1ModelFactory;

    public Projection_viewMapper(Projection_viewCreator<T> creator, Test1Model.Factory<T1> test1ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : test1ModelFactory.column2Adapter.decode(cursor.getLong(0)),
          cursor.isNull(1) ? null : test1ModelFactory.column2Adapter.decode(cursor.getLong(1))
      );
    }
  }

  final class Test2_projectionMapper<T extends Test2_projectionModel, T1 extends Test2Model> implements RowMapper<T> {
    private final Test2_projectionCreator<T> creator;

    private final Factory<T1> test2ModelFactory;

    public Test2_projectionMapper(Test2_projectionCreator<T> creator, Factory<T1> test2ModelFactory) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.column2Adapter.decode(cursor.getString(0))
      );
    }
  }

  interface Creator<T extends Test2Model> {
    T create(@Nullable Long _id, @NonNull String column1, @NonNull List column2);
  }

  final class Mapper<T extends Test2Model> implements RowMapper<T> {
    private final Factory<T> test2ModelFactory;

    public Mapper(Factory<T> test2ModelFactory) {
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test2ModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getString(1),
          test2ModelFactory.column2Adapter.decode(cursor.getString(2))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List, String> column2Adapter;

    Marshal(@Nullable Test2Model copy, ColumnAdapter<List, String> column2Adapter) {
      this.column2Adapter = column2Adapter;
      if (copy != null) {
        this._id(copy._id());
        this.column1(copy.column1());
        this.column2(copy.column2());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(Long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal column1(String column1) {
      contentValues.put("column1", column1);
      return this;
    }

    public Marshal column2(@NonNull List column2) {
      contentValues.put("column2", column2Adapter.encode(column2));
      return this;
    }
  }

  final class Factory<T extends Test2Model> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, String> column2Adapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, String> column2Adapter) {
      this.creator = creator;
      this.column2Adapter = column2Adapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, column2Adapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(Test2Model copy) {
      return new Marshal(copy, column2Adapter);
    }

    public <V2 extends Test1Model.View1Model, R extends Other_selectModel<T, V2>> Other_selectMapper<T, V2, R> other_selectMapper(Other_selectCreator<T, V2, R> creator, Test1Model.View1Creator<V2> view1Creator) {
      return new Other_selectMapper<T, V2, R>(creator, this, view1Creator);
    }

    public <R extends Test1Model.View1Model> Test1Model.View1Mapper<R> view_selectMapper(Test1Model.View1Creator<R> creator) {
      return new Test1Model.View1Mapper<R>(creator);
    }

    public <R extends Test2_copyModel<T>> Test2_copyMapper<T, R> copy_view_selectMapper(Test2_copyCreator<T, R> creator) {
      return new Test2_copyMapper<T, R>(creator, this);
    }

    public <V1 extends Test2_copyModel<T>, V2T1 extends Test1Model, V2 extends Multiple_tablesModel<V2T1, T>, R extends Multiple_view_selectModel<T, V1, V2T1, V2>> Multiple_view_selectMapper<T, V1, V2T1, V2, R> multiple_view_selectMapper(Multiple_view_selectCreator<T, V1, V2T1, V2, R> creator, Test2_copyCreator<T, V1> test2_copyCreator, Test1Model.Factory<V2T1> test1ModelFactory, Multiple_tablesCreator<V2T1, T, V2> multiple_tablesCreator) {
      return new Multiple_view_selectMapper<T, V1, V2T1, V2, R>(creator, this, test2_copyCreator, test1ModelFactory, multiple_tablesCreator);
    }

    public <V1 extends Test1Model.View1Model, R extends Views_and_columns_selectModel<V1>> Views_and_columns_selectMapper<V1, R> views_and_columns_selectMapper(Views_and_columns_selectCreator<V1, R> creator, Test1Model.View1Creator<V1> view1Creator) {
      return new Views_and_columns_selectMapper<V1, R>(creator, view1Creator);
    }

    public <V1V1 extends Test1Model.View1Model, V1 extends Sub_viewModel<V1V1>, V2 extends Test2_copyModel<T>, R extends Select_from_sub_viewModel<V1V1, V1, T, V2>> Select_from_sub_viewMapper<V1V1, V1, T, V2, R> select_from_sub_viewMapper(Select_from_sub_viewCreator<V1V1, V1, T, V2, R> creator, Sub_viewCreator<V1V1, V1> sub_viewCreator, Test1Model.View1Creator<V1V1> view1Creator, Test2_copyCreator<T, V2> test2_copyCreator) {
      return new Select_from_sub_viewMapper<V1V1, V1, T, V2, R>(creator, sub_viewCreator, view1Creator, this, test2_copyCreator);
    }

    public <R extends Projection_viewModel, T1 extends Test1Model> Projection_viewMapper<R, T1> select_from_projectionMapper(Projection_viewCreator<R> creator, Test1Model.Factory<T1> test1ModelFactory) {
      return new Projection_viewMapper<R, T1>(creator, test1ModelFactory);
    }

    public <R extends Test2_projectionModel> Test2_projectionMapper<R, T> select_from_test2_projectionMapper(Test2_projectionCreator<R> creator) {
      return new Test2_projectionMapper<R, T>(creator, this);
    }
  }
}
