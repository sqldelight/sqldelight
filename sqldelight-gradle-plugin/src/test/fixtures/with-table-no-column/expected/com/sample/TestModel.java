package com.sample;

import android.database.Cursor;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String SOME_SELECT = ""
      + "WITH test (cheese) AS (\n"
      + "  VALUES (1)\n"
      + ")\n"
      + "SELECT *\n"
      + "FROM test";

  final class Factory {
    public Factory() {
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
