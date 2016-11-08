package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

public interface Test1Model {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String NULLABLE_TEXT = "nullable_text";

  String NONNULL_TEXT = "nonnull_text";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  nullable_text TEXT,\n"
      + "  nonnull_text TEXT NOT NULL\n"
      + ")";

  long _id();

  @Nullable
  String nullable_text();

  @NonNull
  String nonnull_text();

  interface Join_tableModel<T1 extends Test1Model, T2 extends Test2Model> {
    @NonNull
    T1 test();

    @NonNull
    T2 test2();
  }

  interface Join_tableCreator<T1 extends Test1Model, T2 extends Test2Model, T extends Join_tableModel<T1, T2>> {
    T create(@NonNull T1 test, @NonNull T2 test2);
  }

  final class Join_tableMapper<T1 extends Test1Model, T2 extends Test2Model, T extends Join_tableModel<T1, T2>> implements RowMapper<T> {
    private final Join_tableCreator<T1, T2, T> creator;

    private final Factory<T1> test1ModelFactory;

    private final Test2Model.Factory<T2> test2ModelFactory;

    public Join_tableMapper(Join_tableCreator<T1, T2, T> creator, Factory<T1> test1ModelFactory, Test2Model.Factory<T2> test2ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getString(1),
              cursor.getString(2)
          ),
          test2ModelFactory.creator.create(
              cursor.getLong(3),
              cursor.isNull(4) ? null : cursor.getLong(4),
              cursor.getLong(5),
              cursor.getLong(6)
          )
      );
    }
  }

  interface Left_join_tableModel<T1 extends Test1Model, T2 extends Test2Model> {
    @NonNull
    T1 test();

    @Nullable
    T2 test2();
  }

  interface Left_join_tableCreator<T1 extends Test1Model, T2 extends Test2Model, T extends Left_join_tableModel<T1, T2>> {
    T create(@NonNull T1 test, @Nullable T2 test2);
  }

  final class Left_join_tableMapper<T1 extends Test1Model, T2 extends Test2Model, T extends Left_join_tableModel<T1, T2>> implements RowMapper<T> {
    private final Left_join_tableCreator<T1, T2, T> creator;

    private final Factory<T1> test1ModelFactory;

    private final Test2Model.Factory<T2> test2ModelFactory;

    public Left_join_tableMapper(Left_join_tableCreator<T1, T2, T> creator, Factory<T1> test1ModelFactory, Test2Model.Factory<T2> test2ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getString(1),
              cursor.getString(2)
          ),
          cursor.isNull(3)
              ? null
              : test2ModelFactory.creator.create(
                  cursor.getLong(3),
                  cursor.isNull(4) ? null : cursor.getLong(4),
                  cursor.getLong(5),
                  cursor.getLong(6)
              )
      );
    }
  }

  interface Join_table_columnsModel<T1 extends Test1Model> {
    @NonNull
    T1 test();

    long _id();

    @Nullable
    Long nullable_int();

    long nonnull_int();
  }

  interface Join_table_columnsCreator<T1 extends Test1Model, T extends Join_table_columnsModel<T1>> {
    T create(@NonNull T1 test, long _id, @Nullable Long nullable_int, long nonnull_int);
  }

  final class Join_table_columnsMapper<T1 extends Test1Model, T extends Join_table_columnsModel<T1>> implements RowMapper<T> {
    private final Join_table_columnsCreator<T1, T> creator;

    private final Factory<T1> test1ModelFactory;

    public Join_table_columnsMapper(Join_table_columnsCreator<T1, T> creator, Factory<T1> test1ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getString(1),
              cursor.getString(2)
          ),
          cursor.getLong(3),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.getLong(5)
      );
    }
  }

  interface Left_join_table_columnsModel<T1 extends Test1Model> {
    @NonNull
    T1 test();

    @Nullable
    Long _id();

    @Nullable
    Long nullable_int();

    @Nullable
    Long nonnull_int();
  }

  interface Left_join_table_columnsCreator<T1 extends Test1Model, T extends Left_join_table_columnsModel<T1>> {
    T create(@NonNull T1 test, @Nullable Long _id, @Nullable Long nullable_int, @Nullable Long nonnull_int);
  }

  final class Left_join_table_columnsMapper<T1 extends Test1Model, T extends Left_join_table_columnsModel<T1>> implements RowMapper<T> {
    private final Left_join_table_columnsCreator<T1, T> creator;

    private final Factory<T1> test1ModelFactory;

    public Left_join_table_columnsMapper(Left_join_table_columnsCreator<T1, T> creator, Factory<T1> test1ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : cursor.getString(1),
              cursor.getString(2)
          ),
          cursor.isNull(3) ? null : cursor.getLong(3),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.isNull(5) ? null : cursor.getLong(5)
      );
    }
  }

  interface Creator<T extends Test1Model> {
    T create(long _id, @Nullable String nullable_text, @NonNull String nonnull_text);
  }

  final class Mapper<T extends Test1Model> implements RowMapper<T> {
    private final Factory<T> test1ModelFactory;

    public Mapper(Factory<T> test1ModelFactory) {
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test1ModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1),
          cursor.getString(2)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable Test1Model copy) {
      if (copy != null) {
        this._id(copy._id());
        this.nullable_text(copy.nullable_text());
        this.nonnull_text(copy.nonnull_text());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal nullable_text(String nullable_text) {
      contentValues.put("nullable_text", nullable_text);
      return this;
    }

    public Marshal nonnull_text(String nonnull_text) {
      contentValues.put("nonnull_text", nonnull_text);
      return this;
    }
  }

  final class Factory<T extends Test1Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(Test1Model copy) {
      return new Marshal(copy);
    }

    public SqlDelightStatement join_table() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test\n"
          + "JOIN test2",
          new String[0], Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test","test2"))));
    }

    public SqlDelightStatement left_join_table() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test\n"
          + "LEFT JOIN test2",
          new String[0], Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test","test2"))));
    }

    public SqlDelightStatement join_table_columns() {
      return new SqlDelightStatement(""
          + "SELECT test.*, test2._id, nullable_int, nonnull_int\n"
          + "FROM test\n"
          + "JOIN test2",
          new String[0], Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test","test2"))));
    }

    public SqlDelightStatement left_join_table_columns() {
      return new SqlDelightStatement(""
          + "SELECT test.*, test2._id, nullable_int, nonnull_int\n"
          + "FROM test\n"
          + "LEFT JOIN test2",
          new String[0], Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test","test2"))));
    }

    public <T2 extends Test2Model, R extends Join_tableModel<T, T2>> Join_tableMapper<T, T2, R> join_tableMapper(Join_tableCreator<T, T2, R> creator, Test2Model.Factory<T2> test2ModelFactory) {
      return new Join_tableMapper<T, T2, R>(creator, this, test2ModelFactory);
    }

    public <T2 extends Test2Model, R extends Left_join_tableModel<T, T2>> Left_join_tableMapper<T, T2, R> left_join_tableMapper(Left_join_tableCreator<T, T2, R> creator, Test2Model.Factory<T2> test2ModelFactory) {
      return new Left_join_tableMapper<T, T2, R>(creator, this, test2ModelFactory);
    }

    public <R extends Join_table_columnsModel<T>> Join_table_columnsMapper<T, R> join_table_columnsMapper(Join_table_columnsCreator<T, R> creator) {
      return new Join_table_columnsMapper<T, R>(creator, this);
    }

    public <R extends Left_join_table_columnsModel<T>> Left_join_table_columnsMapper<T, R> left_join_table_columnsMapper(Left_join_table_columnsCreator<T, R> creator) {
      return new Left_join_table_columnsMapper<T, R>(creator, this);
    }
  }
}
