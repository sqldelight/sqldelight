package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.SqliteLiterals;
import java.lang.Boolean;
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

  String SOME_BOOL = "some_bool";

  String SOME_ENUM = "some_enum";

  String SOME_BLOB = "some_blob";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  some_bool INTEGER,\n"
      + "  some_enum TEXT,\n"
      + "  some_blob BLOB DEFAULT '0x01'\n"
      + ")";

  long _id();

  @Nullable
  Boolean some_bool();

  @Nullable
  Test.TestEnum some_enum();

  @Nullable
  byte[] some_blob();

  interface Creator<T extends TestModel> {
    T create(long _id, @Nullable Boolean some_bool, @Nullable Test.TestEnum some_enum,
        @Nullable byte[] some_blob);
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
          cursor.isNull(1) ? null : cursor.getInt(1) == 1,
          cursor.isNull(2) ? null : testModelFactory.some_enumAdapter.decode(cursor.getString(2)),
          cursor.isNull(3) ? null : cursor.getBlob(3)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Test.TestEnum, String> some_enumAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<Test.TestEnum, String> some_enumAdapter) {
      this.some_enumAdapter = some_enumAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.some_bool(copy.some_bool());
        this.some_enum(copy.some_enum());
        this.some_blob(copy.some_blob());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal some_bool(Boolean some_bool) {
      if (some_bool == null) {
        contentValues.putNull("some_bool");
        return this;
      }
      contentValues.put("some_bool", some_bool ? 1 : 0);
      return this;
    }

    public Marshal some_enum(@Nullable Test.TestEnum some_enum) {
      if (some_enum != null) {
        contentValues.put("some_enum", some_enumAdapter.encode(some_enum));
      } else {
        contentValues.putNull("some_enum");
      }
      return this;
    }

    public Marshal some_blob(byte[] some_blob) {
      contentValues.put("some_blob", some_blob);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Test.TestEnum, String> some_enumAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Test.TestEnum, String> some_enumAdapter) {
      this.creator = creator;
      this.some_enumAdapter = some_enumAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, some_enumAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, some_enumAdapter);
    }

    /**
     * @deprecated Use {@link Insert_new_row}
     */
    @Deprecated
    public SqlDelightStatement insert_new_row(@Nullable Boolean some_bool,
        @Nullable Test.TestEnum some_enum, @Nullable byte[] some_blob) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("INSERT INTO test (some_bool, some_enum, some_blob)\n"
              + "VALUES (");
      if (some_bool == null) {
        query.append("null");
      } else {
        query.append(some_bool ? 1 : 0);
      }
      query.append(", ");
      if (some_enum == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) some_enumAdapter.encode(some_enum));
      }
      query.append(", ");
      if (some_blob == null) {
        query.append("null");
      } else {
        query.append(SqliteLiterals.forBlob(some_blob));
      }
      query.append(")");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    /**
     * @deprecated Use {@link Trigger_stuff}
     */
    @Deprecated
    public SqlDelightStatement trigger_stuff(@Nullable Boolean some_bool, long arg2) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("CREATE TRIGGER some_trigger\n"
              + "BEFORE UPDATE ON test\n"
              + "BEGIN\n"
              + "  UPDATE test\n"
              + "  SET some_bool = ");
      if (some_bool == null) {
        query.append("null");
      } else {
        query.append(some_bool ? 1 : 0);
      }
      query.append("\n"
              + "  WHERE ");
      query.append(arg2);
      query.append(";\n"
              + "END");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }
  }

  final class Insert_new_row extends SqlDelightCompiledStatement.Insert {
    private final Factory<? extends TestModel> testModelFactory;

    public Insert_new_row(SQLiteDatabase database, Factory<? extends TestModel> testModelFactory) {
      super("test", database.compileStatement(""
              + "INSERT INTO test (some_bool, some_enum, some_blob)\n"
              + "VALUES (?, ?, ?)"));
      this.testModelFactory = testModelFactory;
    }

    public void bind(@Nullable Boolean some_bool, @Nullable Test.TestEnum some_enum,
        @Nullable byte[] some_blob) {
      if (some_bool == null) {
        program.bindNull(1);
      } else {
        program.bindLong(1, some_bool ? 1 : 0);
      }
      if (some_enum == null) {
        program.bindNull(2);
      } else {
        program.bindString(2, testModelFactory.some_enumAdapter.encode(some_enum));
      }
      if (some_blob == null) {
        program.bindNull(3);
      } else {
        program.bindBlob(3, some_blob);
      }
    }
  }

  final class Trigger_stuff extends SqlDelightCompiledStatement {
    public Trigger_stuff(SQLiteDatabase database) {
      super("test", database.compileStatement(""
              + "CREATE TRIGGER some_trigger\n"
              + "BEFORE UPDATE ON test\n"
              + "BEGIN\n"
              + "  UPDATE test\n"
              + "  SET some_bool = ?\n"
              + "  WHERE ?;\n"
              + "END"));
    }

    public void bind(@Nullable Boolean some_bool, long arg2) {
      if (some_bool == null) {
        program.bindNull(1);
      } else {
        program.bindLong(1, some_bool ? 1 : 0);
      }
      program.bindLong(2, arg2);
    }
  }
}
