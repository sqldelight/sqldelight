package com.sample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.SqliteLiterals;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    T create(@NonNull String column1, @NonNull String column2, @Nullable String column3,
        @Nullable String column4, @Nullable String column5, @Nullable String column6,
        @Nullable byte[] column7, long column8, @NonNull String column9, @Nullable String column10,
        @Nullable String column11);
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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * @deprecated Use {@link Update_row}
     */
    @Deprecated
    public SqlDelightStatement update_row(@NonNull String column2, @Nullable String column3,
        @Nullable String column4, @Nullable String column5, @Nullable String column6,
        @Nullable byte[] column7, long column8, @NonNull String column9, @Nullable String column11,
        @NonNull String column1) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("UPDATE test_table\n"
              + "SET column2 = ");
      query.append('?').append(currentIndex++);
      args.add(column2);
      query.append(",\n"
              + "    column3 = ");
      if (column3 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column3);
      }
      query.append(",\n"
              + "    column4 = ");
      if (column4 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column4);
      }
      query.append(",\n"
              + "    column5 = ");
      if (column5 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column5);
      }
      query.append(",\n"
              + "    column6 = ");
      if (column6 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column6);
      }
      query.append(",\n"
              + "    column7 = ");
      if (column7 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append(SqliteLiterals.forBlob(column7));
      }
      query.append(",\n"
              + "    column8 = ");
      query.append(column8);
      query.append(",\n"
              + "    column9 = ");
      query.append('?').append(currentIndex++);
      args.add(column9);
      query.append(",\n"
              + "    column11 = ");
      if (column11 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column11);
      }
      query.append("\n"
              + "WHERE column1 = ");
      query.append('?').append(currentIndex++);
      args.add(column1);
      query.append("\n"
              + "AND column7 < ");
      query.append(column8);
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test_table"));
    }

    /**
     * @deprecated Use {@link Update_row_with_name}
     */
    @Deprecated
    public SqlDelightStatement update_row_with_name(@NonNull String column2,
        @Nullable String column3, @Nullable String column4, @Nullable String column5,
        @Nullable String column6, @Nullable byte[] column7, long column8, @NonNull String column9,
        @Nullable String column11, @NonNull String column1) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("UPDATE test_table\n"
              + "SET column2 = ");
      query.append('?').append(currentIndex++);
      args.add(column2);
      query.append(",\n"
              + "    column3 = ");
      if (column3 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column3);
      }
      query.append(",\n"
              + "    column4 = ");
      if (column4 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column4);
      }
      query.append(",\n"
              + "    column5 = ");
      if (column5 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column5);
      }
      query.append(",\n"
              + "    column6 = ");
      if (column6 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column6);
      }
      query.append(",\n"
              + "    column7 = ");
      if (column7 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append(SqliteLiterals.forBlob(column7));
      }
      query.append(",\n"
              + "    column8 = ");
      query.append(column8);
      query.append(",\n"
              + "    column9 = ");
      query.append('?').append(currentIndex++);
      args.add(column9);
      query.append(",\n"
              + "    column11 = ");
      if (column11 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(column11);
      }
      query.append("\n"
              + "WHERE column1 = ");
      query.append('?').append(currentIndex++);
      args.add(column1);
      query.append("\n"
              + "AND column7 < ");
      if (column7 == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append(SqliteLiterals.forBlob(column7));
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test_table"));
    }
  }

  final class Update_row extends SqlDelightCompiledStatement.Update {
    public Update_row(SQLiteDatabase database) {
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

  final class Update_row_with_name extends SqlDelightCompiledStatement.Update {
    public Update_row_with_name(SQLiteDatabase database) {
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
