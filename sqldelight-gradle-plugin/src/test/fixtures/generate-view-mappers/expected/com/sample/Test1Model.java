package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface Test1Model {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String COLUMN1 = "column1";

  String COLUMN2 = "column2";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  column1 TEXT,\n"
      + "  column2 INTEGER\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW view1 AS\n"
      + "SELECT max(column2) AS max, _id\n"
      + "FROM test";

  String SOME_SELECT = ""
      + "SELECT *\n"
      + "FROM view1";

  String OTHER_SELECT = ""
      + "SELECT *\n"
      + "FROM view1\n"
      + "JOIN test USING (_id)";

  String SAME_VIEW = ""
      + "SELECT *\n"
      + "FROM view1 first_view\n"
      + "JOIN view1 second_view";

  @Nullable
  Long _id();

  @Nullable
  String column1();

  @Nullable
  Long column2();

  interface Other_selectModel {
    View1Model view1();

    Test1Model test();
  }

  interface Other_selectCreator<T extends Other_selectModel> {
    T create(View1Model view1, Test1Model test);
  }

  final class Other_selectMapper<T extends Other_selectModel, R1 extends Test1Model, V1 extends View1Model> implements RowMapper<T> {
    private final Other_selectCreator<T> creator;

    private final Factory<R1> test1ModelFactory;

    private final View1Creator<V1> view1Creator;

    private Other_selectMapper(Other_selectCreator<T> creator, Factory<R1> test1ModelFactory, View1Creator<V1> view1Creator) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
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
          test1ModelFactory.creator.create(
              cursor.isNull(2) ? null : cursor.getLong(2),
              cursor.isNull(3) ? null : cursor.getString(3),
              cursor.isNull(4) ? null : cursor.getLong(4)
          )
      );
    }
  }

  interface Same_viewModel {
    View1Model first_view();

    View1Model second_view();
  }

  interface Same_viewCreator<T extends Same_viewModel> {
    T create(View1Model first_view, View1Model second_view);
  }

  final class Same_viewMapper<T extends Same_viewModel, V1 extends View1Model> implements RowMapper<T> {
    private final Same_viewCreator<T> creator;

    private final View1Creator<V1> view1Creator;

    private Same_viewMapper(Same_viewCreator<T> creator, View1Creator<V1> view1Creator) {
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
          view1Creator.create(
              cursor.isNull(2) ? null : cursor.getLong(2),
              cursor.isNull(3) ? null : cursor.getLong(3)
          )
      );
    }
  }

  interface View1Model {
    long max();

    Long _id();
  }

  interface View1Creator<T extends View1Model> {
    T create(long max, Long _id);
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
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getLong(1)
      );
    }
  }

  interface Creator<T extends Test1Model> {
    T create(Long _id, String column1, Long column2);
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
          cursor.isNull(1) ? null : cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getLong(2)
      );
    }
  }

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public Marshal() {
    }

    public Marshal(Test1Model copy) {
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

    public T column2(Long column2) {
      contentValues.put(COLUMN2, column2);
      return (T) this;
    }
  }

  final class Factory<T extends Test1Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public <R extends View1Model> View1Mapper<R> some_selectMapper(View1Creator<R> creator) {
      return new View1Mapper<>(creator);
    }

    public <R extends Other_selectModel, V1 extends View1Model> Other_selectMapper<R, T, V1> other_selectMapper(Other_selectCreator<R> creator, View1Creator<V1> view1Creator) {
      return new Other_selectMapper<>(creator, this, view1Creator);
    }

    public <R extends Same_viewModel, V1 extends View1Model> Same_viewMapper<R, V1> same_viewMapper(Same_viewCreator<R> creator, View1Creator<V1> view1Creator) {
      return new Same_viewMapper<>(creator, view1Creator);
    }
  }
}
