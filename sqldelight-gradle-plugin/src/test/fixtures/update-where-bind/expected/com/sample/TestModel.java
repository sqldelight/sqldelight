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
  String TABLE_NAME = "test_table";

  @Deprecated
  String COLUMN1 = "column1";

  @Deprecated
  String COLUMN2 = "column2";

  @Deprecated
  String COLUMN3 = "column3";

  @Deprecated
  String COLUMN4 = "column4";

  @Deprecated
  String COLUMN5 = "column5";

  @Deprecated
  String COLUMN6 = "column6";

  @Deprecated
  String COLUMN7 = "column7";

  @Deprecated
  String COLUMN8 = "column8";

  @Deprecated
  String COLUMN9 = "column9";

  @Deprecated
  String COLUMN10 = "column10";

  @Deprecated
  String COLUMN11 = "column11";

  String CREATE_TABLE = ""
      + "CREATE TABLE test_table (\n"
      + "  column1 TEXT NOT NULL PRIMARY KEY,\n"
      + "  column2 TEXT NOT NULL,\n"
      + "  column3 TEXT,\n"
      + "  column4 TEXT,\n"
      + "  column5 TEXT,\n"
      + "  column6 TEXT,\n"
      + "  column7 BLOB,\n"
      + "  column8 INTEGER NOT NULL,\n"
      + "  column9 TEXT NOT NULL,\n"
      + "  column10 TEXT,\n"
      + "  column11 TEXT\n"
      + ")";

  @NonNull
  String column1();

  @NonNull
  String column2();

  @Nullable
  String column3();

  @Nullable
  String column4();

  @Nullable
  String column5();

  @Nullable
  String column6();

  @Nullable
  byte[] column7();

  long column8();

  @NonNull
  String column9();

  @Nullable
  String column10();

  @Nullable
  String column11();

  interface Creator<T extends TestModel> {
    T create(@NonNull String column1, @NonNull String column2, @Nullable String column3,
        @Nullable String column4, @Nullable String column5, @Nullable String column6,
        @Nullable byte[] column7, long column8, @NonNull String column9, @Nullable String column10,
        @Nullable String column11);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(@NonNull Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getString(0),
          cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getString(4),
          cursor.isNull(5) ? null : cursor.getString(5),
          cursor.isNull(6) ? null : cursor.getBlob(6),
          cursor.getLong(7),
          cursor.getString(8),
          cursor.isNull(9) ? null : cursor.getString(9),
          cursor.isNull(10) ? null : cursor.getString(10)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Update_row extends SqlDelightStatement {
    public Update_row(@NonNull SupportSQLiteDatabase database) {
      super("test_table", database.compileStatement(""
              + "UPDATE test_table\n"
              + "SET column2 = ?1,\n"
              + "    column3 = ?2,\n"
              + "    column4 = ?3,\n"
              + "    column5 = ?4,\n"
              + "    column6 = ?5,\n"
              + "    column7 = ?6,\n"
              + "    column8 = ?7,\n"
              + "    column9 = ?8,\n"
              + "    column11 = ?9\n"
              + "WHERE column1 = ?10\n"
              + "AND column7 < ?7"));
    }

    public void bind(@NonNull String column2, @Nullable String column3, @Nullable String column4,
        @Nullable String column5, @Nullable String column6, @Nullable byte[] column7, long column8,
        @NonNull String column9, @Nullable String column11, @NonNull String column1) {
      bindString(1, column2);
      if (column3 == null) {
        bindNull(2);
      } else {
        bindString(2, column3);
      }
      if (column4 == null) {
        bindNull(3);
      } else {
        bindString(3, column4);
      }
      if (column5 == null) {
        bindNull(4);
      } else {
        bindString(4, column5);
      }
      if (column6 == null) {
        bindNull(5);
      } else {
        bindString(5, column6);
      }
      if (column7 == null) {
        bindNull(6);
      } else {
        bindBlob(6, column7);
      }
      bindLong(7, column8);
      bindString(8, column9);
      if (column11 == null) {
        bindNull(9);
      } else {
        bindString(9, column11);
      }
      bindString(10, column1);
    }
  }

  final class Update_row_with_name extends SqlDelightStatement {
    public Update_row_with_name(@NonNull SupportSQLiteDatabase database) {
      super("test_table", database.compileStatement(""
              + "UPDATE test_table\n"
              + "SET column2 = ?,\n"
              + "    column3 = ?,\n"
              + "    column4 = ?,\n"
              + "    column5 = ?,\n"
              + "    column6 = ?,\n"
              + "    column7 = :column7,\n"
              + "    column8 = ?,\n"
              + "    column9 = ?,\n"
              + "    column11 = ?\n"
              + "WHERE column1 = ?\n"
              + "AND column7 < :column7"));
    }

    public void bind(@NonNull String column2, @Nullable String column3, @Nullable String column4,
        @Nullable String column5, @Nullable String column6, @Nullable byte[] column7, long column8,
        @NonNull String column9, @Nullable String column11, @NonNull String column1) {
      bindString(1, column2);
      if (column3 == null) {
        bindNull(2);
      } else {
        bindString(2, column3);
      }
      if (column4 == null) {
        bindNull(3);
      } else {
        bindString(3, column4);
      }
      if (column5 == null) {
        bindNull(4);
      } else {
        bindString(4, column5);
      }
      if (column6 == null) {
        bindNull(5);
      } else {
        bindString(5, column6);
      }
      if (column7 == null) {
        bindNull(6);
      } else {
        bindBlob(6, column7);
      }
      bindLong(7, column8);
      bindString(8, column9);
      if (column11 == null) {
        bindNull(9);
      } else {
        bindString(9, column11);
      }
      bindString(10, column1);
    }
  }
}
