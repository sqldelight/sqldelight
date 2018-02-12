package com.sample;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  name TEXT NOT NULL DEFAULT 'alec'\n"
      + ")";

  long _id();

  @NonNull
  String name();

  interface Creator<T extends TestModel> {
    T create(long _id, @NonNull String name);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Insert_default_values extends SqlDelightStatement {
    public Insert_default_values(@NonNull SupportSQLiteDatabase database) {
      super("test", database.compileStatement(""
              + "INSERT INTO test DEFAULT VALUES"));
    }
  }
}
