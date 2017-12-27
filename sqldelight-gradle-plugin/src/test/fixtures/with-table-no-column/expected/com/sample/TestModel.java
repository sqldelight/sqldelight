package com.sample;

import android.database.Cursor;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;

public interface TestModel {
  final class Factory {
    public Factory() {
    }

    public SqlDelightQuery some_select() {
      return new SqlDelightQuery(""
          + "WITH test (cheese) AS (\n"
          + "  VALUES (1)\n"
          + ")\n"
          + "SELECT *\n"
          + "FROM test",
          Collections.<String>emptySet());
    }

    public RowMapper<Long> some_selectMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }
  }
}
