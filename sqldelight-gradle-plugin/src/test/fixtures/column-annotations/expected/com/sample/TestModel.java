package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.List;

public interface TestModel {
  String TABLE_NAME = "test";

  String DEPRECATED = "deprecated";

  String SUPPRESSED_WARNINGS = "suppressed_warnings";

  String SUPPRESSED_WARNINGS_VALUE = "suppressed_warnings_value";

  String CLASS_ANNOTATION = "class_annotation";

  String INTEGER_ANNOTATION = "integer_annotation";

  String STRING_ARRAY_ANNOTATION = "string_array_annotation";

  String MULTIPLE_VALUES_ANNOTATION = "multiple_values_annotation";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  deprecated TEXT,\n"
      + "  suppressed_warnings INTEGER NOT NULL,\n"
      + "  suppressed_warnings_value INTEGER,\n"
      + "  class_annotation TEXT,\n"
      + "  integer_annotation INTEGER,\n"
      + "  string_array_annotation TEXT,\n"
      + "  multiple_values_annotation TEXT NOT NULL\n"
      + ")";

  @Nullable
  @Deprecated
  String deprecated();

  @SuppressWarnings("sup")
  long suppressed_warnings();

  @Nullable
  @SuppressWarnings("sup")
  Long suppressed_warnings_value();

  @Nullable
  @ClassAnnotation(List.class)
  String class_annotation();

  @Nullable
  @IntegerAnnotation(1)
  Integer integer_annotation();

  @Nullable
  @StringArrayAnnotation({"Alec","Matt","Jake"})
  String string_array_annotation();

  @NonNull
  @MultipleValuesAnnotation(
      value1 = 1,
      value2 = "sup",
      value3 = {"Alec","Matt","Jake"},
      value4 = List.class
  )
  String multiple_values_annotation();

  interface Creator<T extends TestModel> {
    T create(@Nullable String deprecated, long suppressed_warnings, @Nullable Long suppressed_warnings_value, @Nullable String class_annotation, @Nullable Integer integer_annotation, @Nullable String string_array_annotation, @NonNull String multiple_values_annotation);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getString(0),
          cursor.getLong(1),
          cursor.isNull(2) ? null : cursor.getLong(2),
          cursor.isNull(3) ? null : cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getInt(4),
          cursor.isNull(5) ? null : cursor.getString(5),
          cursor.getString(6)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this.deprecated(copy.deprecated());
        this.suppressed_warnings(copy.suppressed_warnings());
        this.suppressed_warnings_value(copy.suppressed_warnings_value());
        this.class_annotation(copy.class_annotation());
        this.integer_annotation(copy.integer_annotation());
        this.string_array_annotation(copy.string_array_annotation());
        this.multiple_values_annotation(copy.multiple_values_annotation());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal deprecated(String deprecated) {
      contentValues.put("deprecated", deprecated);
      return this;
    }

    public Marshal suppressed_warnings(long suppressed_warnings) {
      contentValues.put("suppressed_warnings", suppressed_warnings);
      return this;
    }

    public Marshal suppressed_warnings_value(Long suppressed_warnings_value) {
      contentValues.put("suppressed_warnings_value", suppressed_warnings_value);
      return this;
    }

    public Marshal class_annotation(String class_annotation) {
      contentValues.put("class_annotation", class_annotation);
      return this;
    }

    public Marshal integer_annotation(Integer integer_annotation) {
      contentValues.put("integer_annotation", integer_annotation);
      return this;
    }

    public Marshal string_array_annotation(String string_array_annotation) {
      contentValues.put("string_array_annotation", string_array_annotation);
      return this;
    }

    public Marshal multiple_values_annotation(String multiple_values_annotation) {
      contentValues.put("multiple_values_annotation", multiple_values_annotation);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }
  }
}
