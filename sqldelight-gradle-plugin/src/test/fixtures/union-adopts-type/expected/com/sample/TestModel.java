package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Calendar;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String NULLABLE_TEXT = "nullable_text";

  @Deprecated
  String NONNULL_TEXT = "nonnull_text";

  @Deprecated
  String NULLABLE_INT = "nullable_int";

  @Deprecated
  String NONNULL_INT = "nonnull_int";

  @Deprecated
  String CUSTOM_TYPE = "custom_type";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  nullable_text TEXT,\n"
      + "  nonnull_text TEXT NOT NULL,\n"
      + "  nullable_int INTEGER,\n"
      + "  nonnull_int INTEGER NOT NULL,\n"
      + "  custom_type INTEGER NOT NULL\n"
      + ")";

  long _id();

  @Nullable
  String nullable_text();

  @NonNull
  String nonnull_text();

  @Nullable
  Long nullable_int();

  long nonnull_int();

  @NonNull
  Calendar custom_type();

  interface Union_nullabilityModel {
    @Nullable
    String nonnull_text();

    @Nullable
    String test_nonnull_text();
  }

  interface Union_nullabilityCreator<T extends Union_nullabilityModel> {
    T create(@Nullable String nonnull_text, @Nullable String test_nonnull_text);
  }

  final class Union_nullabilityMapper<T extends Union_nullabilityModel> implements RowMapper<T> {
    private final Union_nullabilityCreator<T> creator;

    public Union_nullabilityMapper(Union_nullabilityCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Union_typeModel {
    @NonNull
    String nonnull_int();

    @Nullable
    String nullable_int();
  }

  interface Union_typeCreator<T extends Union_typeModel> {
    T create(@NonNull String nonnull_int, @Nullable String nullable_int);
  }

  final class Union_typeMapper<T extends Union_typeModel> implements RowMapper<T> {
    private final Union_typeCreator<T> creator;

    public Union_typeMapper(Union_typeCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Union_tables_for_some_reasonModel {
    long _id();

    @Nullable
    String nullable_text();

    @Nullable
    String nonnull_text();

    @Nullable
    Long nullable_int();

    @Nullable
    Long nonnull_int();

    @Nullable
    Calendar custom_type();
  }

  interface Union_tables_for_some_reasonCreator<T extends Union_tables_for_some_reasonModel> {
    T create(long _id, @Nullable String nullable_text, @Nullable String nonnull_text,
        @Nullable Long nullable_int, @Nullable Long nonnull_int, @Nullable Calendar custom_type);
  }

  final class Union_tables_for_some_reasonMapper<T extends Union_tables_for_some_reasonModel, T1 extends TestModel> implements RowMapper<T> {
    private final Union_tables_for_some_reasonCreator<T> creator;

    private final Factory<T1> testModelFactory;

    public Union_tables_for_some_reasonMapper(Union_tables_for_some_reasonCreator<T> creator,
        @NonNull Factory<T1> testModelFactory) {
      this.creator = creator;
      this.testModelFactory = testModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getLong(3),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.isNull(5) ? null : testModelFactory.custom_typeAdapter.decode(cursor.getLong(5))
      );
    }
  }

  interface Union_custom_types_keeps_typeModel {
    @NonNull
    Calendar custom_type();

    @Nullable
    Calendar test_custom_type();
  }

  interface Union_custom_types_keeps_typeCreator<T extends Union_custom_types_keeps_typeModel> {
    T create(@NonNull Calendar custom_type, @Nullable Calendar test_custom_type);
  }

  final class Union_custom_types_keeps_typeMapper<T extends Union_custom_types_keeps_typeModel, T1 extends TestModel> implements RowMapper<T> {
    private final Union_custom_types_keeps_typeCreator<T> creator;

    private final Factory<T1> testModelFactory;

    public Union_custom_types_keeps_typeMapper(Union_custom_types_keeps_typeCreator<T> creator,
        @NonNull Factory<T1> testModelFactory) {
      this.creator = creator;
      this.testModelFactory = testModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          testModelFactory.custom_typeAdapter.decode(cursor.getLong(0)),
          cursor.isNull(1) ? null : testModelFactory.custom_typeAdapter.decode(cursor.getLong(1))
      );
    }
  }

  interface Union_custom_type_uses_datatypeModel {
    @Nullable
    String custom_type();

    @NonNull
    Calendar test_custom_type();
  }

  interface Union_custom_type_uses_datatypeCreator<T extends Union_custom_type_uses_datatypeModel> {
    T create(@Nullable String custom_type, @NonNull Calendar test_custom_type);
  }

  final class Union_custom_type_uses_datatypeMapper<T extends Union_custom_type_uses_datatypeModel, T1 extends TestModel> implements RowMapper<T> {
    private final Union_custom_type_uses_datatypeCreator<T> creator;

    private final Factory<T1> testModelFactory;

    public Union_custom_type_uses_datatypeMapper(Union_custom_type_uses_datatypeCreator<T> creator,
        @NonNull Factory<T1> testModelFactory) {
      this.creator = creator;
      this.testModelFactory = testModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getString(0),
          testModelFactory.custom_typeAdapter.decode(cursor.getLong(1))
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long _id, @Nullable String nullable_text, @NonNull String nonnull_text,
        @Nullable Long nullable_int, long nonnull_int, @NonNull Calendar custom_type);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1),
          cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getLong(3),
          cursor.getLong(4),
          testModelFactory.custom_typeAdapter.decode(cursor.getLong(5))
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar, Long> custom_typeAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<Calendar, Long> custom_typeAdapter) {
      this.creator = creator;
      this.custom_typeAdapter = custom_typeAdapter;
    }

    @NonNull
    public SqlDelightQuery union_nullability() {
      return new SqlDelightQuery(""
          + "SELECT nonnull_text, nonnull_text\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT nullable_text, null\n"
          + "FROM test",
          new TableSet("test"));
    }

    @NonNull
    public SqlDelightQuery union_type() {
      return new SqlDelightQuery(""
          + "SELECT nonnull_int, nullable_int\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT nonnull_text, nonnull_text\n"
          + "FROM test",
          new TableSet("test"));
    }

    @NonNull
    public SqlDelightQuery union_tables_for_some_reason() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM test\n"
          + "UNION\n"
          + "VALUES (1, null, null, null, null, null)",
          new TableSet("test"));
    }

    @NonNull
    public SqlDelightQuery union_custom_types_keeps_type() {
      return new SqlDelightQuery(""
          + "SELECT custom_type, custom_type\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT custom_type, null\n"
          + "FROM test",
          new TableSet("test"));
    }

    @NonNull
    public SqlDelightQuery union_custom_type_uses_datatype() {
      return new SqlDelightQuery(""
          + "SELECT custom_type, custom_type\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT nullable_text, nonnull_int\n"
          + "FROM test",
          new TableSet("test"));
    }

    @NonNull
    public <R extends Union_nullabilityModel> Union_nullabilityMapper<R> union_nullabilityMapper(
        Union_nullabilityCreator<R> creator) {
      return new Union_nullabilityMapper<R>(creator);
    }

    @NonNull
    public <R extends Union_typeModel> Union_typeMapper<R> union_typeMapper(
        Union_typeCreator<R> creator) {
      return new Union_typeMapper<R>(creator);
    }

    @NonNull
    public <R extends Union_tables_for_some_reasonModel> Union_tables_for_some_reasonMapper<R, T> union_tables_for_some_reasonMapper(
        Union_tables_for_some_reasonCreator<R> creator) {
      return new Union_tables_for_some_reasonMapper<R, T>(creator, this);
    }

    @NonNull
    public <R extends Union_custom_types_keeps_typeModel> Union_custom_types_keeps_typeMapper<R, T> union_custom_types_keeps_typeMapper(
        Union_custom_types_keeps_typeCreator<R> creator) {
      return new Union_custom_types_keeps_typeMapper<R, T>(creator, this);
    }

    @NonNull
    public <R extends Union_custom_type_uses_datatypeModel> Union_custom_type_uses_datatypeMapper<R, T> union_custom_type_uses_datatypeMapper(
        Union_custom_type_uses_datatypeCreator<R> creator) {
      return new Union_custom_type_uses_datatypeMapper<R, T>(creator, this);
    }
  }
}
