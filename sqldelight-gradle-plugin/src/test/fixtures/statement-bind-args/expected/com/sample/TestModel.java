package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Boolean;
import java.lang.Override;
import java.lang.String;

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
    T create(long _id, @Nullable Boolean some_bool, @Nullable Test.TestEnum some_enum, @Nullable byte[] some_blob);
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
      contentValues.put(_ID, _id);
      return this;
    }

    public Marshal some_bool(Boolean some_bool) {
      if (some_bool == null) {
        contentValues.putNull(SOME_BOOL);
        return this;
      }
      contentValues.put(SOME_BOOL, some_bool ? 1 : 0);
      return this;
    }

    public Marshal some_enum(@Nullable Test.TestEnum some_enum) {
      if (some_enum != null) {
        contentValues.put(SOME_ENUM, some_enumAdapter.encode(some_enum));
      } else {
        contentValues.putNull(SOME_ENUM);
      }
      return this;
    }

    public Marshal some_blob(byte[] some_blob) {
      contentValues.put(SOME_BLOB, some_blob);
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

    public Marshal marshal() {
      return new Marshal(null, some_enumAdapter);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, some_enumAdapter);
    }

    public void insert_new_row(Insert_new_rowStatement statement, @Nullable Boolean some_bool, @Nullable Test.TestEnum some_enum, @Nullable byte[] some_blob) {
      if (some_bool == null) {
        statement.program.bindNull(1);
      } else {
        statement.program.bindLong(1, some_bool ? 1 : 0);
      }
      if (some_enum == null) {
        statement.program.bindNull(2);
      } else {
        statement.program.bindString(2, some_enumAdapter.encode(some_enum));
      }
      if (some_blob == null) {
        statement.program.bindNull(3);
      } else {
        statement.program.bindBlob(3, some_blob);
      }
    }

    public void trigger_stuff(Trigger_stuffStatement statement, @Nullable Boolean some_bool, long arg2) {
      if (some_bool == null) {
        statement.program.bindNull(1);
      } else {
        statement.program.bindLong(1, some_bool ? 1 : 0);
      }
      statement.program.bindLong(2, arg2);
    }
  }

  final class Insert_new_rowStatement {
    public static final String table = "test";

    public final SQLiteStatement program;

    public Insert_new_rowStatement(SQLiteDatabase database) {
      program = database.compileStatement(""
              + "INSERT INTO test (some_bool, some_enum, some_blob)\n"
              + "VALUES (?, ?, ?)");
    }
  }

  final class Trigger_stuffStatement {
    public static final String table = "test";

    public final SQLiteStatement program;

    public Trigger_stuffStatement(SQLiteDatabase database) {
      program = database.compileStatement(""
              + "CREATE TRIGGER some_trigger\n"
              + "BEFORE UPDATE ON test\n"
              + "BEGIN\n"
              + "  UPDATE test\n"
              + "  SET some_bool = ?\n"
              + "  WHERE ?;\n"
              + "END");
    }
  }
}
