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

public interface TestModel {
  String TABLE_NAME = "test";

  String ID = "id";

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

    public Mapper(Factory<T> testModelFactory) {
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List, Long> dateAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<List, Long> dateAdapter) {
      this.dateAdapter = dateAdapter;
      if (copy != null) {
        this.id(copy.id());
        this.date(copy.date());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal id(Long id) {
      contentValues.put("id", id);
      return this;
    }

    public Marshal date(@NonNull List date) {
      contentValues.put("date", dateAdapter.encode(date));
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, Long> dateAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, Long> dateAdapter) {
      this.creator = creator;
      this.dateAdapter = dateAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, dateAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, dateAdapter);
    }
  }
}
