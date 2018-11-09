package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Calendar;

public interface BookModel {
  @Deprecated
  String TABLE_NAME = "book";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String TITLE = "title";

  @Deprecated
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

    public Mapper(@NonNull Factory<T> bookModelFactory) {
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

  final class Factory<T extends BookModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar, Long> published_atAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<Calendar, Long> published_atAdapter) {
      this.creator = creator;
      this.published_atAdapter = published_atAdapter;
    }

    @NonNull
    public SqlDelightQuery select_latest_date() {
      return new SqlDelightQuery(""
          + "SELECT published_at\n"
          + "FROM book\n"
          + "ORDER BY published_at DESC\n"
          + "LIMIT 1",
          new TableSet("book"));
    }

    @NonNull
    public SqlDelightQuery select_latest_title() {
      return new SqlDelightQuery(""
          + "SELECT title\n"
          + "FROM book\n"
          + "ORDER BY published_at DESC\n"
          + "LIMIT 1",
          new TableSet("book"));
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
