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
  List column2();

  interface Other_selectModel<V1 extends View1Model, T2 extends Test1Model> {
    @NonNull
    V1 view1();

    @NonNull
    T2 test();
  }

  interface Other_selectCreator<V1 extends View1Model, T2 extends Test1Model, T extends Other_selectModel<V1, T2>> {
    T create(@NonNull V1 view1, @NonNull T2 test);
  }

  final class Other_selectMapper<V1 extends View1Model, T2 extends Test1Model, T extends Other_selectModel<V1, T2>> implements RowMapper<T> {
    private final Other_selectCreator<V1, T2, T> creator;

    private final Factory<T2> test1ModelFactory;

    private final View1Creator<V1> view1Creator;

    public Other_selectMapper(Other_selectCreator<V1, T2, T> creator, Factory<T2> test1ModelFactory, View1Creator<V1> view1Creator) {
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
              cursor.isNull(4) ? null : test1ModelFactory.column2Adapter.decode(cursor.getLong(4))
          )
      );
    }
  }

  interface Same_viewModel<V1 extends View1Model> {
    @NonNull
    V1 first_view();

    @NonNull
    V1 second_view();
  }

  interface Same_viewCreator<V1 extends View1Model, T extends Same_viewModel<V1>> {
    T create(@NonNull V1 first_view, @NonNull V1 second_view);
  }

  final class Same_viewMapper<V1 extends View1Model, T extends Same_viewModel<V1>> implements RowMapper<T> {
    private final Same_viewCreator<V1, T> creator;

    private final View1Creator<V1> view1Creator;

    public Same_viewMapper(Same_viewCreator<V1, T> creator, View1Creator<V1> view1Creator) {
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
    @Nullable
    Long max();

    @Nullable
    Long _id();
  }

  interface View1Creator<T extends View1Model> {
    T create(@Nullable Long max, @Nullable Long _id);
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
    T create(@Nullable Long _id, @Nullable String column1, @Nullable List column2);
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
          cursor.isNull(2) ? null : test1ModelFactory.column2Adapter.decode(cursor.getLong(2))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List, Long> column2Adapter;

    Marshal(@Nullable Test1Model copy, ColumnAdapter<List, Long> column2Adapter) {
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

    public Marshal column2(@Nullable List column2) {
      if (column2 != null) {
        contentValues.put("column2", column2Adapter.encode(column2));
      } else {
        contentValues.putNull("column2");
      }
      return this;
    }
  }

  final class Factory<T extends Test1Model> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, Long> column2Adapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, Long> column2Adapter) {
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
    public Marshal marshal(Test1Model copy) {
      return new Marshal(copy, column2Adapter);
    }

    public <R extends View1Model> View1Mapper<R> some_selectMapper(View1Creator<R> creator) {
      return new View1Mapper<R>(creator);
    }

    public <V1 extends View1Model, R extends Other_selectModel<V1, T>> Other_selectMapper<V1, T, R> other_selectMapper(Other_selectCreator<V1, T, R> creator, View1Creator<V1> view1Creator) {
      return new Other_selectMapper<V1, T, R>(creator, this, view1Creator);
    }

    public <V1 extends View1Model, R extends Same_viewModel<V1>> Same_viewMapper<V1, R> same_viewMapper(Same_viewCreator<V1, R> creator, View1Creator<V1> view1Creator) {
      return new Same_viewMapper<V1, R>(creator, view1Creator);
    }
  }
}
