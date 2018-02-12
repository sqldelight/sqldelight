package com.sample;

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
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String DEPRECATED = "deprecated";

  @Deprecated
  String SUPPRESSED_WARNINGS = "suppressed_warnings";

  @Deprecated
  String SUPPRESSED_WARNINGS_VALUE = "suppressed_warnings_value";

  @Deprecated
  String CLASS_ANNOTATION = "class_annotation";

  @Deprecated
  String INTEGER_ANNOTATION = "integer_annotation";

  @Deprecated
  String STRING_ARRAY_ANNOTATION = "string_array_annotation";

  @Deprecated
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
    T create(@Nullable String deprecated, long suppressed_warnings,
        @Nullable Long suppressed_warnings_value, @Nullable String class_annotation,
        @Nullable Integer integer_annotation, @Nullable String string_array_annotation,
        @NonNull String multiple_values_annotation);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }
}
