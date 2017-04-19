package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String SOME_COLUMN = "some_column";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  some_column TEXT\n"
      + ")";

  String SOME_DELETE = ""
      + "DELETE FROM test";

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

  final class Marshal {
    final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this.some_column(copy.some_column());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal some_column(String some_column) {
      contentValues.put("some_column", some_column);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }
  }

  final class Some_delete extends SqlDelightCompiledStatement.Delete {
    public Some_delete(SQLiteDatabase database) {
      super("test", database.compileStatement(""
              + "DELETE FROM test"));
    }
  }
}
