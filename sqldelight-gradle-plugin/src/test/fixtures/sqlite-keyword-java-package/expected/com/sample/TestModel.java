package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import no.test.TestEnum;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test_table";

  @Deprecated
  String TEST_COLUMN = "test_column";

  String CREATE_TABLE = ""
      + "CREATE TABLE test_table (\n"
      + "    test_column TEXT\n"
      + ")";

  @Nullable
  TestEnum test_column();

  interface Creator<T extends TestModel> {
    T create(@Nullable TestEnum test_column);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : testModelFactory.test_columnAdapter.decode(cursor.getString(0))
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<TestEnum, String> test_columnAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<TestEnum, String> test_columnAdapter) {
      this.creator = creator;
      this.test_columnAdapter = test_columnAdapter;
    }
  }
}
