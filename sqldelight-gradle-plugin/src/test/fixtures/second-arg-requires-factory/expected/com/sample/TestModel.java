package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String ID = "id";

  @Deprecated
  String DATE = "date";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  date INTEGER NOT NULL\n"
      + ")";

  @Nullable
  Long id();

  @NonNull
  List date();

  interface Creator<T extends TestModel> {
    T create(@Nullable Long id, @NonNull List date);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          testModelFactory.dateAdapter.decode(cursor.getLong(1))
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, Long> dateAdapter;

    public Factory(@NonNull Creator<T> creator, @NonNull ColumnAdapter<List, Long> dateAdapter) {
      this.creator = creator;
      this.dateAdapter = dateAdapter;
    }
  }
}
