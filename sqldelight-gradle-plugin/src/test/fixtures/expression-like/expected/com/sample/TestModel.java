package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface TestModel {
  String TABLE_NAME = "employee";

  String ID = "id";

  String DEPARTMENT = "department";

  String NAME = "name";

  String TITLE = "title";

  String BIO = "bio";

  String CREATE_TABLE = ""
      + "CREATE TABLE employee (\n"
      + "  id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  department TEXT NOT NULL,\n"
      + "  name TEXT NOT NULL,\n"
      + "  title TEXT NOT NULL,\n"
      + "  bio TEXT NOT NULL\n"
      + ")";

  long id();

  @NonNull
  String department();

  @NonNull
  String name();

  @NonNull
  String title();

  @NonNull
  String bio();

  interface Creator<T extends TestModel> {
    T create(long id, @NonNull String department, @NonNull String name, @NonNull String title,
        @NonNull String bio);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          cursor.getString(2),
          cursor.getString(3),
          cursor.getString(4)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement some_select(@NonNull String department, @Nullable String arg2,
        @Nullable String arg3, @Nullable String arg4) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM employee\n"
              + "WHERE department = ");
      query.append('?').append(currentIndex++);
      args.add(department);
      query.append("\n"
              + "AND (\n"
              + "  name LIKE '%' || ");
      if (arg2 == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(arg2);
      }
      query.append(" || '%'\n"
              + "  OR title LIKE '%' || ");
      if (arg3 == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(arg3);
      }
      query.append(" || '%'\n"
              + "  OR bio LIKE '%' || ");
      if (arg4 == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(arg4);
      }
      query.append(" || '%'\n"
              + ")\n"
              + "ORDER BY department");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("employee"));
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }
  }
}
