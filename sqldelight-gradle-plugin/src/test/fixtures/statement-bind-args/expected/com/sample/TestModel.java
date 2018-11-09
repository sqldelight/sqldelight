package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightStatement;
import java.lang.Boolean;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String SOME_BOOL = "some_bool";

  @Deprecated
  String SOME_ENUM = "some_enum";

  @Deprecated
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

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Test.TestEnum, String> some_enumAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<Test.TestEnum, String> some_enumAdapter) {
      this.creator = creator;
      this.some_enumAdapter = some_enumAdapter;
    }
  }

  final class Insert_new_row extends SqlDelightStatement {
    private final Factory<? extends TestModel> testModelFactory;

    public Insert_new_row(@NonNull SupportSQLiteDatabase database,
        Factory<? extends TestModel> testModelFactory) {
      super("test", database.compileStatement(""
              + "INSERT INTO test (some_bool, some_enum, some_blob)\n"
              + "VALUES (?, ?, ?)"));
      this.testModelFactory = testModelFactory;
    }

    public void bind(@Nullable Boolean some_bool, @Nullable Test.TestEnum some_enum,
        @Nullable byte[] some_blob) {
      if (some_bool == null) {
        bindNull(1);
      } else {
        bindLong(1, some_bool ? 1 : 0);
      }
      if (some_enum == null) {
        bindNull(2);
      } else {
        bindString(2, testModelFactory.some_enumAdapter.encode(some_enum));
      }
      if (some_blob == null) {
        bindNull(3);
      } else {
        bindBlob(3, some_blob);
      }
    }
  }

  final class Trigger_stuff extends SqlDelightStatement {
    public Trigger_stuff(@NonNull SupportSQLiteDatabase database) {
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
        bindNull(1);
      } else {
        bindLong(1, some_bool ? 1 : 0);
      }
      bindLong(2, arg2);
    }
  }
}
