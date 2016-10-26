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
          bookModelFactory.published_atAdapter.decode(cursor.getLong(2))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar, Long> published_atAdapter;

    Marshal(@Nullable BookModel copy, ColumnAdapter<Calendar, Long> published_atAdapter) {
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
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal title(String title) {
      contentValues.put("title", title);
      return this;
    }

    public Marshal published_at(@NonNull Calendar published_at) {
      contentValues.put("published_at", published_atAdapter.encode(published_at));
      return this;
    }
  }

  final class Factory<T extends BookModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar, Long> published_atAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar, Long> published_atAdapter) {
      this.creator = creator;
      this.published_atAdapter = published_atAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, published_atAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(BookModel copy) {
      return new Marshal(copy, published_atAdapter);
    }

    public SqlDelightStatement select_latest_date() {
      return new SqlDelightStatement(""
          + "SELECT published_at\n"
          + "FROM book\n"
          + "ORDER BY published_at DESC\n"
          + "LIMIT 1",
          new String[0], Collections.<String>singleton("book"));
    }

    public SqlDelightStatement select_latest_title() {
      return new SqlDelightStatement(""
          + "SELECT title\n"
          + "FROM book\n"
          + "ORDER BY published_at DESC\n"
          + "LIMIT 1",
          new String[0], Collections.<String>singleton("book"));
    }

    public RowMapper<Calendar> select_latest_dateMapper() {
      return new RowMapper<Calendar>() {
        @Override
        public Calendar map(Cursor cursor) {
          return published_atAdapter.decode(cursor.getLong(0));
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
