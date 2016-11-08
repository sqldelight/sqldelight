package com.sample;

import android.database.Cursor;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Long;
import java.lang.Override;
import java.util.Collections;

public interface TestModel {
  final class Factory {
    public Factory() {
    }

    public SqlDelightStatement some_select() {
      return new SqlDelightStatement(""
          + "WITH test (cheese) AS (\n"
          + "  VALUES (1)\n"
          + ")\n"
          + "SELECT *\n"
          + "FROM test",
          new String[0], Collections.<String>emptySet());
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
