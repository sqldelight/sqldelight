package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Boolean;
import java.lang.Deprecated;
import java.lang.Double;
import java.lang.Float;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.Short;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
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

    public SqlDelightStatement some_select(Object arg1) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *, ");
      if (!(arg1 instanceof String)) {
        query.append(arg1);
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) arg1);
      }
      query.append("\n"
              + "FROM test");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    /**
     * @deprecated Use {@link Some_delete}
     */
    @Deprecated
    public SqlDelightStatement some_delete(Object arg1) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("WITH rubbish AS (VALUES (");
      if (!(arg1 instanceof String)) {
        query.append(arg1);
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) arg1);
      }
      query.append("))\n"
              + "DELETE FROM test\n"
              + "WHERE _id IN rubbish");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }
  }

  final class Some_delete extends SqlDelightCompiledStatement.Delete {
    public Some_delete(SQLiteDatabase database) {
      super("test", database.compileStatement(""
              + "WITH rubbish AS (VALUES (?))\n"
              + "DELETE FROM test\n"
              + "WHERE _id IN rubbish"));
    }

    public void bind(Object arg1) {
      if (arg1 == null) {
        program.bindNull(1);
      } else if (arg1 instanceof String) {
        program.bindString(1, (String) arg1);
      } else if (arg1 instanceof Float || arg1 instanceof Double) {
        program.bindDouble(1, (double) arg1);
      } else if (arg1 instanceof Integer || arg1 instanceof Short || arg1 instanceof Long) {
        program.bindLong(1, (long) arg1);
      } else if (arg1 instanceof Boolean) {
        program.bindLong(1, (boolean) arg1 ? 1 : 0);
      } else if (arg1 instanceof byte[]) {
        program.bindBlob(1, (byte[]) arg1);
      } else {
        throw new IllegalArgumentException("Attempting to bind an object that is not one of (String, Integer, Short, Long, Float, Double, Boolean, byte[]) to argument arg1");
      }
    }
  }
}
