package com.sample;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Float;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.Short;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long _id();

  interface Creator<T extends TestModel> {
    T create(long _id);
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

    public SqlDelightQuery some_select(@Nullable Object arg1) {
      return new Some_selectQuery(arg1);
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }

    private final class Some_selectQuery extends SqlDelightQuery {
      @Nullable
      private final Object arg1;

      Some_selectQuery(@Nullable Object arg1) {
        super("SELECT *, ?1\n"
            + "FROM test",
            new TableSet("test"));

        this.arg1 = arg1;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Object arg1 = this.arg1;
        if (arg1 != null) {
          if (arg1 instanceof String) {
            program.bindString(1, (String) arg1);
          } else if (arg1 instanceof Long || arg1 instanceof Integer || arg1 instanceof Short) {
            program.bindLong(1, (long) arg1);
          } else if (arg1 instanceof Boolean) {
            program.bindLong(1, (boolean) arg1 ? 0 : 1);
          } else if (arg1 instanceof byte[]) {
            program.bindBlob(1, (byte[]) arg1);
          } else if (arg1 instanceof Float || arg1 instanceof Double) {
            program.bindDouble(1, (double) arg1);
          } else {
            throw new IllegalArgumentException("Attempting to bind an object that is not one of String, Integer, Short, Long, Float, Double, Boolean, or byte[] to argument arg1");
          }
        } else {
          program.bindNull(1);
        }
      }
    }
  }

  final class Some_delete extends SqlDelightStatement {
    public Some_delete(SupportSQLiteDatabase database) {
      super("test", database.compileStatement(""
              + "WITH rubbish AS (VALUES (?))\n"
              + "DELETE FROM test\n"
              + "WHERE _id IN rubbish"));
    }

    public void bind(Object arg1) {
      if (arg1 == null) {
        bindNull(1);
      } else if (arg1 instanceof String) {
        bindString(1, (String) arg1);
      } else if (arg1 instanceof Float || arg1 instanceof Double) {
        bindDouble(1, (double) arg1);
      } else if (arg1 instanceof Integer || arg1 instanceof Short || arg1 instanceof Long) {
        bindLong(1, (long) arg1);
      } else if (arg1 instanceof Boolean) {
        bindLong(1, (boolean) arg1 ? 1 : 0);
      } else if (arg1 instanceof byte[]) {
        bindBlob(1, (byte[]) arg1);
      } else {
        throw new IllegalArgumentException("Attempting to bind an object that is not one of (String, Integer, Short, Long, Float, Double, Boolean, byte[]) to argument arg1");
      }
    }
  }
}
