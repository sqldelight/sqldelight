package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;

public interface TestModel {
  final class Factory {
    public Factory() {
    }

    @NonNull
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
