package com.sample;

import android.database.Cursor;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String SOMESTRING = "someString";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id\tINTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  someString\tTEXT\n"
      + ")";

  long _id();

  @Nullable
  String someString();

  interface Creator<T extends TestModel> {
    T create(long _id, @Nullable String someString);
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
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Some_delete extends SqlDelightCompiledStatement {
    public Some_delete(SupportSQLiteDatabase database) {
      super("test2", database.compileStatement(""
              + "DELETE FROM test2 WHERE (SELECT someString FROM test WHERE test._id = test2.testId) = ?"));
    }

    public void bind(@Nullable String someString) {
      if (someString == null) {
        program.bindNull(1);
      } else {
        program.bindString(1, someString);
      }
    }
  }
}
