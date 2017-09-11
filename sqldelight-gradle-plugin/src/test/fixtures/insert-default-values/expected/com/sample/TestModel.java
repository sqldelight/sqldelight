package com.sample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  name TEXT NOT NULL DEFAULT 'alec'\n"
      + ")";

  String INSERT_DEFAULT_VALUES = ""
      + "INSERT INTO test DEFAULT VALUES";

  long _id();

  @NonNull
  String name();

  interface Creator<T extends TestModel> {
    T create(long _id, @NonNull String name);
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
          cursor.getString(1)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Insert_default_values extends SqlDelightCompiledStatement {
    public Insert_default_values(SQLiteDatabase database) {
      super("test", database.compileStatement(""
              + "INSERT INTO test DEFAULT VALUES"));
    }
  }
}
