package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String SOME_TEXT = "some_text";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  some_text TEXT NOT NULL\n"
      + ")";

  String SOME_SELECT = ""
      + "SELECT CASE _id WHEN 0 THEN some_text ELSE some_text + _id END AS indexed_text\n"
      + "FROM test";

  @Nullable
  Long _id();

  @NonNull
  String some_text();

  interface Creator<T extends TestModel> {
    T create(@Nullable Long _id, @NonNull String some_text);
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
          cursor.getString(1)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.some_text(copy.some_text());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(Long _id) {
      contentValues.put(_ID, _id);
      return this;
    }

    public Marshal some_text(String some_text) {
      contentValues.put(SOME_TEXT, some_text);
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

    public RowMapper<String> some_selectMapper() {
      return new RowMapper<String>() {
        @Override
        public String map(Cursor cursor) {
          return cursor.getString(0);
        }
      };
    }
  }
}
