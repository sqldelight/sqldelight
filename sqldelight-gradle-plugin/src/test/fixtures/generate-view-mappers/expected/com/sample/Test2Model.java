package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

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

  @Nullable
  Long _id();

  @NonNull
  String column1();

  @NonNull
  String column2();

  interface Other_selectModel {
    Test2Model test2();

    Test1Model.View1Model view1();
  }

  interface Other_selectCreator<T extends Other_selectModel> {
    T create(Test2Model test2, Test1Model.View1Model view1);
  }

  final class Other_selectMapper<T extends Other_selectModel, R1 extends Test2Model, V1 extends Test1Model.View1Model> implements RowMapper<T> {
    private final Other_selectCreator<T> creator;

    private final Factory<R1> test2ModelFactory;

    private final Test1Model.View1Creator<V1> view1Creator;

    private Other_selectMapper(Other_selectCreator<T> creator, Factory<R1> test2ModelFactory, Test1Model.View1Creator<V1> view1Creator) {
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
              cursor.getString(2)
          ),
          view1Creator.create(
              cursor.isNull(3) ? null : cursor.getLong(3),
              cursor.isNull(4) ? null : cursor.getLong(4)
          )
      );
    }
  }

  interface View_selectModel {
    Test1Model.View1Model view1();
  }

  interface View_selectCreator<T extends View_selectModel> {
    T create(Test1Model.View1Model view1);
  }

  final class View_selectMapper<T extends View_selectModel, V1 extends Test1Model.View1Model> implements RowMapper<T> {
    private final View_selectCreator<T> creator;

    private final Test1Model.View1Creator<V1> view1Creator;

    private View_selectMapper(View_selectCreator<T> creator, Test1Model.View1Creator<V1> view1Creator) {
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
          )
      );
    }
  }

  interface Copy_view_selectModel {
    Test2_copyModel test2_copy();
  }

  interface Copy_view_selectCreator<T extends Copy_view_selectModel> {
    T create(Test2_copyModel test2_copy);
  }

  final class Copy_view_selectMapper<T extends Copy_view_selectModel, V1R1 extends Test2Model, V1 extends Test2_copyModel> implements RowMapper<T> {
    private final Copy_view_selectCreator<T> creator;

    private final Factory<V1R1> test2ModelFactory;

    private final Test2_copyCreator<V1> test2_copyCreator;

    private Copy_view_selectMapper(Copy_view_selectCreator<T> creator, Factory<V1R1> test2ModelFactory, Test2_copyCreator<V1> test2_copyCreator) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.test2_copyCreator = test2_copyCreator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2_copyCreator.create(
              test2ModelFactory.creator.create(
                  cursor.isNull(0) ? null : cursor.getLong(0),
                  cursor.getString(1),
                  cursor.getString(2)
              )
          )
      );
    }
  }

  interface Multiple_view_selectModel {
    Test2_copyModel test2_copy();

    Multiple_tablesModel multiple_tables();
  }

  interface Multiple_view_selectCreator<T extends Multiple_view_selectModel> {
    T create(Test2_copyModel test2_copy, Multiple_tablesModel multiple_tables);
  }

  final class Multiple_view_selectMapper<T extends Multiple_view_selectModel, V1R1 extends Test2Model, V1 extends Test2_copyModel, V2R1 extends Test1Model, V2 extends Multiple_tablesModel> implements RowMapper<T> {
    private final Multiple_view_selectCreator<T> creator;

    private final Factory<V1R1> test2ModelFactory;

    private final Test2_copyCreator<V1> test2_copyCreator;

    private final Test1Model.Factory<V2R1> test1ModelFactory;

    private final Multiple_tablesCreator<V2> multiple_tablesCreator;

    private Multiple_view_selectMapper(Multiple_view_selectCreator<T> creator, Factory<V1R1> test2ModelFactory, Test2_copyCreator<V1> test2_copyCreator, Test1Model.Factory<V2R1> test1ModelFactory, Multiple_tablesCreator<V2> multiple_tablesCreator) {
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
                  cursor.getString(2)
              )
          ),
          multiple_tablesCreator.create(
              test1ModelFactory.creator.create(
                  cursor.isNull(3) ? null : cursor.getLong(3),
                  cursor.isNull(4) ? null : cursor.getString(4),
                  cursor.isNull(5) ? null : cursor.getLong(5)
              ),
              test2ModelFactory.creator.create(
                  cursor.isNull(6) ? null : cursor.getLong(6),
                  cursor.getString(7),
                  cursor.getString(8)
              )
          )
      );
    }
  }

  interface Views_and_columns_selectModel {
    Test1Model.View1Model first_view();

    String string_literal();

    Test1Model.View1Model second_view();
  }

  interface Views_and_columns_selectCreator<T extends Views_and_columns_selectModel> {
    T create(Test1Model.View1Model first_view, String string_literal, Test1Model.View1Model second_view);
  }

  final class Views_and_columns_selectMapper<T extends Views_and_columns_selectModel, V1 extends Test1Model.View1Model> implements RowMapper<T> {
    private final Views_and_columns_selectCreator<T> creator;

    private final Test1Model.View1Creator<V1> view1Creator;

    private Views_and_columns_selectMapper(Views_and_columns_selectCreator<T> creator, Test1Model.View1Creator<V1> view1Creator) {
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
          cursor.isNull(2) ? null : cursor.getString(2),
          view1Creator.create(
              cursor.isNull(3) ? null : cursor.getLong(3),
              cursor.isNull(4) ? null : cursor.getLong(4)
          )
      );
    }
  }

  interface Select_from_sub_viewModel {
    Sub_viewModel sub_view();

    Test2_copyModel test2_copy();

    String string_literal();
  }

  interface Select_from_sub_viewCreator<T extends Select_from_sub_viewModel> {
    T create(Sub_viewModel sub_view, Test2_copyModel test2_copy, String string_literal);
  }

  final class Select_from_sub_viewMapper<T extends Select_from_sub_viewModel, V1R1 extends Test2Model, V1 extends Test2_copyModel, V2 extends Sub_viewModel, V2V1 extends Test1Model.View1Model> implements RowMapper<T> {
    private final Select_from_sub_viewCreator<T> creator;

    private final Factory<V1R1> test2ModelFactory;

    private final Test2_copyCreator<V1> test2_copyCreator;

    private final Sub_viewCreator<V2> sub_viewCreator;

    private final Test1Model.View1Creator<V2V1> view1Creator;

    private Select_from_sub_viewMapper(Select_from_sub_viewCreator<T> creator, Factory<V1R1> test2ModelFactory, Test2_copyCreator<V1> test2_copyCreator, Sub_viewCreator<V2> sub_viewCreator, Test1Model.View1Creator<V2V1> view1Creator) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.test2_copyCreator = test2_copyCreator;
      this.sub_viewCreator = sub_viewCreator;
      this.view1Creator = view1Creator;
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
              cursor.isNull(2) ? null : cursor.getString(2),
              view1Creator.create(
                  cursor.isNull(3) ? null : cursor.getLong(3),
                  cursor.isNull(4) ? null : cursor.getLong(4)
              )
          ),
          test2_copyCreator.create(
              test2ModelFactory.creator.create(
                  cursor.isNull(5) ? null : cursor.getLong(5),
                  cursor.getString(6),
                  cursor.getString(7)
              )
          ),
          cursor.isNull(8) ? null : cursor.getString(8)
      );
    }
  }

  interface View1Model {
    long max();

    Long _id();
  }

  interface View1Creator<T extends Test1Model.View1Model> {
    T create(long max, Long _id);
  }

  interface Test2_copyModel {
    Test2Model test2();
  }

  interface Test2_copyCreator<T extends Test2_copyModel> {
    T create(Test2Model test2);
  }

  interface Multiple_tablesModel {
    Test1Model test();

    Test2Model test2();
  }

  interface Multiple_tablesCreator<T extends Multiple_tablesModel> {
    T create(Test1Model test, Test2Model test2);
  }

  interface Sub_viewModel {
    Test1Model.View1Model first_view();

    String string_literal();

    Test1Model.View1Model second_view();
  }

  interface Sub_viewCreator<T extends Sub_viewModel> {
    T create(Test1Model.View1Model first_view, String string_literal, Test1Model.View1Model second_view);
  }

  interface Creator<T extends Test2Model> {
    T create(Long _id, String column1, String column2);
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
          cursor.getString(2)
      );
    }
  }

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public Marshal() {
    }

    public Marshal(Test2Model copy) {
      this._id(copy._id());
      this.column1(copy.column1());
      this.column2(copy.column2());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(Long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T column1(String column1) {
      contentValues.put(COLUMN1, column1);
      return (T) this;
    }

    public T column2(String column2) {
      contentValues.put(COLUMN2, column2);
      return (T) this;
    }
  }

  final class Factory<T extends Test2Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public <R extends Other_selectModel, V1 extends Test1Model.View1Model> Other_selectMapper<R, T, V1> other_selectMapper(Other_selectCreator<R> creator, Test1Model.View1Creator<V1> view1Creator) {
      return new Other_selectMapper<>(creator, this, view1Creator);
    }

    public <R extends View_selectModel, V1 extends Test1Model.View1Model> View_selectMapper<R, V1> view_selectMapper(View_selectCreator<R> creator, Test1Model.View1Creator<V1> view1Creator) {
      return new View_selectMapper<>(creator, view1Creator);
    }

    public <R extends Copy_view_selectModel, V1 extends Test2_copyModel> Copy_view_selectMapper<R, T, V1> copy_view_selectMapper(Copy_view_selectCreator<R> creator, Test2_copyCreator<V1> test2_copyCreator) {
      return new Copy_view_selectMapper<>(creator, this, test2_copyCreator);
    }

    public <R extends Multiple_view_selectModel, V1 extends Test2_copyModel, V2R1 extends Test1Model, V2 extends Multiple_tablesModel> Multiple_view_selectMapper<R, T, V1, V2R1, V2> multiple_view_selectMapper(Multiple_view_selectCreator<R> creator, Test2_copyCreator<V1> test2_copyCreator, Test1Model.Factory<V2R1> test1ModelFactory, Multiple_tablesCreator<V2> multiple_tablesCreator) {
      return new Multiple_view_selectMapper<>(creator, this, test2_copyCreator, test1ModelFactory, multiple_tablesCreator);
    }

    public <R extends Views_and_columns_selectModel, V1 extends Test1Model.View1Model> Views_and_columns_selectMapper<R, V1> views_and_columns_selectMapper(Views_and_columns_selectCreator<R> creator, Test1Model.View1Creator<V1> view1Creator) {
      return new Views_and_columns_selectMapper<>(creator, view1Creator);
    }

    public <R extends Select_from_sub_viewModel, V1 extends Test2_copyModel, V2 extends Sub_viewModel, V2V1 extends Test1Model.View1Model> Select_from_sub_viewMapper<R, T, V1, V2, V2V1> select_from_sub_viewMapper(Select_from_sub_viewCreator<R> creator, Test2_copyCreator<V1> test2_copyCreator, Sub_viewCreator<V2> sub_viewCreator, Test1Model.View1Creator<V2V1> view1Creator) {
      return new Select_from_sub_viewMapper<>(creator, this, test2_copyCreator, sub_viewCreator, view1Creator);
    }
  }
}
