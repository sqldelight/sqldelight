package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test_table";

  String COLUMN1 = "column1";

  String COLUMN2 = "column2";

  String COLUMN3 = "column3";

  String COLUMN4 = "column4";

  String COLUMN5 = "column5";

  String COLUMN6 = "column6";

  String COLUMN7 = "column7";

  String COLUMN8 = "column8";

  String COLUMN9 = "column9";

  String COLUMN10 = "column10";

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
    T create(@NonNull String column1, @NonNull String column2, @Nullable String column3, @Nullable String column4, @Nullable String column5, @Nullable String column6, @Nullable byte[] column7, long column8, @NonNull String column9, @Nullable String column10, @Nullable String column11);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this.column1(copy.column1());
        this.column2(copy.column2());
        this.column3(copy.column3());
        this.column4(copy.column4());
        this.column5(copy.column5());
        this.column6(copy.column6());
        this.column7(copy.column7());
        this.column8(copy.column8());
        this.column9(copy.column9());
        this.column10(copy.column10());
        this.column11(copy.column11());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal column1(String column1) {
      contentValues.put("column1", column1);
      return this;
    }

    public Marshal column2(String column2) {
      contentValues.put("column2", column2);
      return this;
    }

    public Marshal column3(String column3) {
      contentValues.put("column3", column3);
      return this;
    }

    public Marshal column4(String column4) {
      contentValues.put("column4", column4);
      return this;
    }

    public Marshal column5(String column5) {
      contentValues.put("column5", column5);
      return this;
    }

    public Marshal column6(String column6) {
      contentValues.put("column6", column6);
      return this;
    }

    public Marshal column7(byte[] column7) {
      contentValues.put("column7", column7);
      return this;
    }

    public Marshal column8(long column8) {
      contentValues.put("column8", column8);
      return this;
    }

    public Marshal column9(String column9) {
      contentValues.put("column9", column9);
      return this;
    }

    public Marshal column10(String column10) {
      contentValues.put("column10", column10);
      return this;
    }

    public Marshal column11(String column11) {
      contentValues.put("column11", column11);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }
  }

  final class Update_row {
    public static final String table = "test_table";

    public final SQLiteStatement program;

    public Update_row(SQLiteDatabase database) {
      program = database.compileStatement(""
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
              + "AND column7 < ?7");
    }

    public void bind(@NonNull String column2, @Nullable String column3, @Nullable String column4, @Nullable String column5, @Nullable String column6, @Nullable byte[] column7, long column8, @NonNull String column9, @Nullable String column11, @NonNull String column1) {
      program.bindString(1, column2);
      if (column3 == null) {
        program.bindNull(2);
      } else {
        program.bindString(2, column3);
      }
      if (column4 == null) {
        program.bindNull(3);
      } else {
        program.bindString(3, column4);
      }
      if (column5 == null) {
        program.bindNull(4);
      } else {
        program.bindString(4, column5);
      }
      if (column6 == null) {
        program.bindNull(5);
      } else {
        program.bindString(5, column6);
      }
      if (column7 == null) {
        program.bindNull(6);
      } else {
        program.bindBlob(6, column7);
      }
      program.bindLong(7, column8);
      program.bindString(8, column9);
      if (column11 == null) {
        program.bindNull(9);
      } else {
        program.bindString(9, column11);
      }
      program.bindString(10, column1);
    }
  }

  final class Update_row_with_name {
    public static final String table = "test_table";

    public final SQLiteStatement program;

    public Update_row_with_name(SQLiteDatabase database) {
      program = database.compileStatement(""
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
              + "AND column7 < :column7");
    }

    public void bind(@NonNull String column2, @Nullable String column3, @Nullable String column4, @Nullable String column5, @Nullable String column6, @Nullable byte[] column7, long column8, @NonNull String column9, @Nullable String column11, @NonNull String column1) {
      program.bindString(1, column2);
      if (column3 == null) {
        program.bindNull(2);
      } else {
        program.bindString(2, column3);
      }
      if (column4 == null) {
        program.bindNull(3);
      } else {
        program.bindString(3, column4);
      }
      if (column5 == null) {
        program.bindNull(4);
      } else {
        program.bindString(4, column5);
      }
      if (column6 == null) {
        program.bindNull(5);
      } else {
        program.bindString(5, column6);
      }
      if (column7 == null) {
        program.bindNull(6);
      } else {
        program.bindBlob(6, column7);
      }
      program.bindLong(7, column8);
      program.bindString(8, column9);
      if (column11 == null) {
        program.bindNull(9);
      } else {
        program.bindString(9, column11);
      }
      program.bindString(10, column1);
    }
  }
}
