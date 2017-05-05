package com.sample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * @deprecated Use {@link Some_delete}
     */
    @Deprecated
    public SqlDelightStatement some_delete(@Nullable String someString) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("DELETE FROM test2 WHERE (SELECT someString FROM test WHERE test._id = test2.testId) = ");
      if (someString == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(someString);
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test2"));
    }
  }

  final class Some_delete extends SqlDelightCompiledStatement.Delete {
    public Some_delete(SQLiteDatabase database) {
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
