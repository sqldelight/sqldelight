package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Collections;

public interface TestModel {
  String TABLE_NAME = "test";

  String ID = "id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long id();

  interface Creator<T extends TestModel> {
    T create(long id);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement select_by_id(long id) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE id = ");
      query.append(id);
      query.append("\n"
              + "LIMIT 1");
      return new SqlDelightStatement(query.toString(), new String[0], Collections.<String>singleton("test"));
    }

    public Mapper<T> select_by_idMapper() {
      return new Mapper<T>(this);
    }
  }
}
