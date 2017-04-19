package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Calendar;
import java.util.Collections;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String NULLABLE_TEXT = "nullable_text";

  String NONNULL_TEXT = "nonnull_text";

  String NULLABLE_INT = "nullable_int";

  String NONNULL_INT = "nonnull_int";

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
        Factory<T1> testModelFactory) {
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
        Factory<T1> testModelFactory) {
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
        Factory<T1> testModelFactory) {
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

    public Mapper(Factory<T> testModelFactory) {
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

  final class Marshal {
    final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar, Long> custom_typeAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<Calendar, Long> custom_typeAdapter) {
      this.custom_typeAdapter = custom_typeAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.nullable_text(copy.nullable_text());
        this.nonnull_text(copy.nonnull_text());
        this.nullable_int(copy.nullable_int());
        this.nonnull_int(copy.nonnull_int());
        this.custom_type(copy.custom_type());
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

    public Marshal nullable_int(Long nullable_int) {
      contentValues.put("nullable_int", nullable_int);
      return this;
    }

    public Marshal nonnull_int(long nonnull_int) {
      contentValues.put("nonnull_int", nonnull_int);
      return this;
    }

    public Marshal custom_type(@NonNull Calendar custom_type) {
      contentValues.put("custom_type", custom_typeAdapter.encode(custom_type));
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar, Long> custom_typeAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar, Long> custom_typeAdapter) {
      this.creator = creator;
      this.custom_typeAdapter = custom_typeAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, custom_typeAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, custom_typeAdapter);
    }

    public SqlDelightStatement union_nullability() {
      return new SqlDelightStatement(""
          + "SELECT nonnull_text, nonnull_text\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT nullable_text, null\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    public SqlDelightStatement union_type() {
      return new SqlDelightStatement(""
          + "SELECT nonnull_int, nullable_int\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT nonnull_text, nonnull_text\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    public SqlDelightStatement union_tables_for_some_reason() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test\n"
          + "UNION\n"
          + "VALUES (1, null, null, null, null, null)",
          new String[0], Collections.<String>singleton("test"));
    }

    public SqlDelightStatement union_custom_types_keeps_type() {
      return new SqlDelightStatement(""
          + "SELECT custom_type, custom_type\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT custom_type, null\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    public SqlDelightStatement union_custom_type_uses_datatype() {
      return new SqlDelightStatement(""
          + "SELECT custom_type, custom_type\n"
          + "FROM test\n"
          + "UNION\n"
          + "SELECT nullable_text, nonnull_int\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    public <R extends Union_nullabilityModel> Union_nullabilityMapper<R> union_nullabilityMapper(Union_nullabilityCreator<R> creator) {
      return new Union_nullabilityMapper<R>(creator);
    }

    public <R extends Union_typeModel> Union_typeMapper<R> union_typeMapper(Union_typeCreator<R> creator) {
      return new Union_typeMapper<R>(creator);
    }

    public <R extends Union_tables_for_some_reasonModel> Union_tables_for_some_reasonMapper<R, T> union_tables_for_some_reasonMapper(Union_tables_for_some_reasonCreator<R> creator) {
      return new Union_tables_for_some_reasonMapper<R, T>(creator, this);
    }

    public <R extends Union_custom_types_keeps_typeModel> Union_custom_types_keeps_typeMapper<R, T> union_custom_types_keeps_typeMapper(Union_custom_types_keeps_typeCreator<R> creator) {
      return new Union_custom_types_keeps_typeMapper<R, T>(creator, this);
    }

    public <R extends Union_custom_type_uses_datatypeModel> Union_custom_type_uses_datatypeMapper<R, T> union_custom_type_uses_datatypeMapper(Union_custom_type_uses_datatypeCreator<R> creator) {
      return new Union_custom_type_uses_datatypeMapper<R, T>(creator, this);
    }
  }
}
