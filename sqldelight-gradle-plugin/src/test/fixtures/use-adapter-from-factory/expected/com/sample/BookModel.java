package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;
import java.util.Calendar;

public interface BookModel {
  String TABLE_NAME = "book";

  String _ID = "_id";

  String TITLE = "title";

  String PUBLISHED_AT = "published_at";

  String CREATE_TABLE = ""
      + "CREATE TABLE book (\n"
      + "   _id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  title TEXT NOT NULL,\n"
      + "  published_at INTEGER NOT NULL\n"
      + ")";

  String SELECT_LATEST_DATE = ""
      + "SELECT published_at\n"
      + "FROM book\n"
      + "ORDER BY published_at DESC\n"
      + "LIMIT 1";

  String SELECT_LATEST_TITLE = ""
      + "SELECT title\n"
      + "FROM book\n"
      + "ORDER BY published_at DESC\n"
      + "LIMIT 1";

  long _id();

  @NonNull
  String title();

  @NonNull
  Calendar published_at();

  interface Creator<T extends BookModel> {
    T create(long _id, @NonNull String title, @NonNull Calendar published_at);
  }

  final class Mapper<T extends BookModel> implements RowMapper<T> {
    private final Factory<T> bookModelFactory;

    public Mapper(Factory<T> bookModelFactory) {
      this.bookModelFactory = bookModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return bookModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          bookModelFactory.published_atAdapter.map(cursor, 2)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar> published_atAdapter;

    Marshal(@Nullable BookModel copy, ColumnAdapter<Calendar> published_atAdapter) {
      this.published_atAdapter = published_atAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.title(copy.title());
        this.published_at(copy.published_at());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put(_ID, _id);
      return this;
    }

    public Marshal title(String title) {
      contentValues.put(TITLE, title);
      return this;
    }

    public Marshal published_at(@NonNull Calendar published_at) {
      published_atAdapter.marshal(contentValues, PUBLISHED_AT, published_at);
      return this;
    }
  }

  final class Factory<T extends BookModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar> published_atAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar> published_atAdapter) {
      this.creator = creator;
      this.published_atAdapter = published_atAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, published_atAdapter);
    }

    public Marshal marshal(BookModel copy) {
      return new Marshal(copy, published_atAdapter);
    }

    public RowMapper<Calendar> select_latest_dateMapper() {
      return new RowMapper<Calendar>() {
        @Override
        public Calendar map(Cursor cursor) {
          return published_atAdapter.map(cursor, 0);
        }
      };
    }

    public RowMapper<String> select_latest_titleMapper() {
      return new RowMapper<String>() {
        @Override
        public String map(Cursor cursor) {
          return cursor.getString(0);
        }
      };
    }
  }
}
