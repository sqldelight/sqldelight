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

  String NULLABLE_INT = "nullable_int";

  String NONNULL_INT = "nonnull_int";

  String TEST2_ID = "test2_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test2 (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  nullable_int INTEGER,\n"
      + "  nonnull_int INTEGER NOT NULL,\n"
      + "  test2_id INTEGER NOT NULL REFERENCES test\n"
      + ")";

  String SOME_VIEW = ""
      + "CREATE VIEW view1 AS\n"
      + "SELECT nullable_int, nonnull_int\n"
      + "FROM test2";

  String JOIN_VIEW = ""
      + "SELECT *\n"
      + "FROM test2\n"
      + "JOIN view1";

  String JOIN_VIEW_COLUMNS = ""
      + "SELECT test2.*, view1.nullable_int, view1.nonnull_int\n"
      + "FROM test2\n"
      + "JOIN view1";

  String LEFT_JOIN_VIEW = ""
      + "SELECT *\n"
      + "FROM test2\n"
      + "LEFT JOIN view1";

  String LEFT_JOIN_VIEW_COLUMNS = ""
      + "SELECT test2.*, view1.nullable_int, view1.nonnull_int\n"
      + "FROM test2\n"
      + "LEFT JOIN view1";

  long _id();

  @Nullable
  Long nullable_int();

  long nonnull_int();

  long test2_id();

  interface Join_viewModel<T1 extends Test2Model, V2 extends View1Model> {
    @NonNull
    T1 test2();

    @NonNull
    V2 view1();
  }

  interface Join_viewCreator<T1 extends Test2Model, V2 extends View1Model, T extends Join_viewModel<T1, V2>> {
    T create(@NonNull T1 test2, @NonNull V2 view1);
  }

  final class Join_viewMapper<T1 extends Test2Model, V2 extends View1Model, T extends Join_viewModel<T1, V2>> implements RowMapper<T> {
    private final Join_viewCreator<T1, V2, T> creator;

    private final Factory<T1> test2ModelFactory;

    private final View1Creator<V2> view1Creator;

    public Join_viewMapper(Join_viewCreator<T1, V2, T> creator, Factory<T1> test2ModelFactory, View1Creator<V2> view1Creator) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.view1Creator = view1Creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getLong(1),
              cursor.getLong(2),
              cursor.getLong(3)
          ),
          view1Creator.create(
              cursor.isNull(4) ? null : cursor.getLong(4),
              cursor.getLong(5)
          )
      );
    }
  }

  interface Join_view_columnsModel<T1 extends Test2Model> {
    @NonNull
    T1 test2();

    @Nullable
    Long nullable_int();

    long nonnull_int();
  }

  interface Join_view_columnsCreator<T1 extends Test2Model, T extends Join_view_columnsModel<T1>> {
    T create(@NonNull T1 test2, @Nullable Long nullable_int, long nonnull_int);
  }

  final class Join_view_columnsMapper<T1 extends Test2Model, T extends Join_view_columnsModel<T1>> implements RowMapper<T> {
    private final Join_view_columnsCreator<T1, T> creator;

    private final Factory<T1> test2ModelFactory;

    public Join_view_columnsMapper(Join_view_columnsCreator<T1, T> creator, Factory<T1> test2ModelFactory) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getLong(1),
              cursor.getLong(2),
              cursor.getLong(3)
          ),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.getLong(5)
      );
    }
  }

  interface Left_join_viewModel<T1 extends Test2Model, V2 extends View1Model> {
    @NonNull
    T1 test2();

    @Nullable
    V2 view1();
  }

  interface Left_join_viewCreator<T1 extends Test2Model, V2 extends View1Model, T extends Left_join_viewModel<T1, V2>> {
    T create(@NonNull T1 test2, @Nullable V2 view1);
  }

  final class Left_join_viewMapper<T1 extends Test2Model, V2 extends View1Model, T extends Left_join_viewModel<T1, V2>> implements RowMapper<T> {
    private final Left_join_viewCreator<T1, V2, T> creator;

    private final Factory<T1> test2ModelFactory;

    private final View1Creator<V2> view1Creator;

    public Left_join_viewMapper(Left_join_viewCreator<T1, V2, T> creator, Factory<T1> test2ModelFactory, View1Creator<V2> view1Creator) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.view1Creator = view1Creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getLong(1),
              cursor.getLong(2),
              cursor.getLong(3)
          ),
          cursor.isNull(5)
              ? null
              : view1Creator.create(
                  cursor.isNull(4) ? null : cursor.getLong(4),
                  cursor.getLong(5)
              )
      );
    }
  }

  interface Left_join_view_columnsModel<T1 extends Test2Model> {
    @NonNull
    T1 test2();

    @Nullable
    Long nullable_int();

    long nonnull_int();
  }

  interface Left_join_view_columnsCreator<T1 extends Test2Model, T extends Left_join_view_columnsModel<T1>> {
    T create(@NonNull T1 test2, @Nullable Long nullable_int, long nonnull_int);
  }

  final class Left_join_view_columnsMapper<T1 extends Test2Model, T extends Left_join_view_columnsModel<T1>> implements RowMapper<T> {
    private final Left_join_view_columnsCreator<T1, T> creator;

    private final Factory<T1> test2ModelFactory;

    public Left_join_view_columnsMapper(Left_join_view_columnsCreator<T1, T> creator, Factory<T1> test2ModelFactory) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getLong(1),
              cursor.getLong(2),
              cursor.getLong(3)
          ),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.getLong(5)
      );
    }
  }

  interface View1Model {
    @Nullable
    Long nullable_int();

    long nonnull_int();
  }

  interface View1Creator<T extends View1Model> {
    T create(@Nullable Long nullable_int, long nonnull_int);
  }

  interface Creator<T extends Test2Model> {
    T create(long _id, @Nullable Long nullable_int, long nonnull_int, long test2_id);
  }

  final class Mapper<T extends Test2Model> implements RowMapper<T> {
    private final Factory<T> test2ModelFactory;

    public Mapper(Factory<T> test2ModelFactory) {
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test2ModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getLong(1),
          cursor.getLong(2),
          cursor.getLong(3)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable Test2Model copy) {
      if (copy != null) {
        this._id(copy._id());
        this.nullable_int(copy.nullable_int());
        this.nonnull_int(copy.nonnull_int());
        this.test2_id(copy.test2_id());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal nullable_int(Long nullable_int) {
      contentValues.put("nullable_int", nullable_int);
      return this;
    }

    public Marshal nonnull_int(long nonnull_int) {
      contentValues.put("nonnull_int", nonnull_int);
      return this;
    }

    public Marshal test2_id(long test2_id) {
      contentValues.put("test2_id", test2_id);
      return this;
    }
  }

  final class Factory<T extends Test2Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(Test2Model copy) {
      return new Marshal(copy);
    }

    public <V2 extends View1Model, R extends Join_viewModel<T, V2>> Join_viewMapper<T, V2, R> join_viewMapper(Join_viewCreator<T, V2, R> creator, View1Creator<V2> view1Creator) {
      return new Join_viewMapper<T, V2, R>(creator, this, view1Creator);
    }

    public <R extends Join_view_columnsModel<T>> Join_view_columnsMapper<T, R> join_view_columnsMapper(Join_view_columnsCreator<T, R> creator) {
      return new Join_view_columnsMapper<T, R>(creator, this);
    }

    public <V2 extends View1Model, R extends Left_join_viewModel<T, V2>> Left_join_viewMapper<T, V2, R> left_join_viewMapper(Left_join_viewCreator<T, V2, R> creator, View1Creator<V2> view1Creator) {
      return new Left_join_viewMapper<T, V2, R>(creator, this, view1Creator);
    }

    public <R extends Left_join_view_columnsModel<T>> Left_join_view_columnsMapper<T, R> left_join_view_columnsMapper(Left_join_view_columnsCreator<T, R> creator) {
      return new Left_join_view_columnsMapper<T, R>(creator, this);
    }
  }
}
