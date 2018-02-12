package com.sample;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Some_delete extends SqlDelightStatement {
    public Some_delete(@NonNull SupportSQLiteDatabase database) {
      super("test2", database.compileStatement(""
              + "DELETE FROM test2 WHERE (SELECT someString FROM test WHERE test._id = test2.testId) = ?"));
    }

    public void bind(@Nullable String someString) {
      if (someString == null) {
        bindNull(1);
      } else {
        bindString(1, someString);
      }
    }
  }
}
