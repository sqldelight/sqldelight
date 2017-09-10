package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface TestModel {
  String TABLE_NAME = "SomeTable";

  String _ID = "_id";

  String TITLE = "title";

  String OTHER_FIELD = "other_field";

  String CREATE_TABLE = ""
      + "CREATE TABLE SomeTable (\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  title TEXT,\n"
      + "  other_field INTEGER\n"
      + ")";

  @Nullable
  Long _id();

  @Nullable
  String title();

  @Nullable
  Long other_field();

  interface Creator<T extends TestModel> {
    T create(@Nullable Long _id, @Nullable String title, @Nullable Long other_field);
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
          cursor.isNull(1) ? null : cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getLong(2)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement select_title(@Nullable String title) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM SomeTable\n"
              + "WHERE title = ");
      if (title == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(title);
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("SomeTable"));
    }

    public Mapper<T> select_titleMapper() {
      return new Mapper<T>(this);
    }
  }
}
