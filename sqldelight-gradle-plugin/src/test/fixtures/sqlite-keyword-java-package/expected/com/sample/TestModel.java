package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;
import no.test.TestEnum;

public interface TestModel {
  String TABLE_NAME = "test_table";

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

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : testModelFactory.test_columnAdapter.decode(cursor.getString(0))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<TestEnum, String> test_columnAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<TestEnum, String> test_columnAdapter) {
      this.test_columnAdapter = test_columnAdapter;
      if (copy != null) {
        this.test_column(copy.test_column());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal test_column(@Nullable TestEnum test_column) {
      if (test_column != null) {
        contentValues.put("test_column", test_columnAdapter.encode(test_column));
      } else {
        contentValues.putNull("test_column");
      }
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<TestEnum, String> test_columnAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<TestEnum, String> test_columnAdapter) {
      this.creator = creator;
      this.test_columnAdapter = test_columnAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, test_columnAdapter);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, test_columnAdapter);
    }
  }
}
