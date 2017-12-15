package com.sample;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String SOME_COLUMN = "some_column";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  some_column TEXT\n"
      + ")";

  @Nullable
  String some_column();

  interface Creator<T extends TestModel> {
    T create(@Nullable String some_column);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getString(0)
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
      super("test", database.compileStatement(""
              + "DELETE FROM test"));
    }
  }
}
