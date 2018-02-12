package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Double;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "some_table";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String QUANTITY = "quantity";

  @Deprecated
  String SOME_REAL = "some_real";

  String CREATE_TABLE = ""
      + "CREATE TABLE some_table (\n"
      + "    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "    quantity INTEGER NOT NULL DEFAULT 0,\n"
      + "    some_real REAL NOT NULL DEFAULT 1.0\n"
      + ")";

  long _id();

  int quantity();

  double some_real();

  interface Creator<T extends TestModel> {
    T create(long _id, int quantity, double some_real);
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
          cursor.getInt(1),
          cursor.getDouble(2)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery get_sum() {
      return new SqlDelightQuery(""
          + "SELECT sum(quantity)\n"
          + "FROM some_table",
          new TableSet("some_table"));
    }

    @NonNull
    public SqlDelightQuery get_rounded() {
      return new SqlDelightQuery(""
          + "SELECT round(some_real)\n"
          + "FROM some_table",
          new TableSet("some_table"));
    }

    @NonNull
    public SqlDelightQuery get_rounded_arg() {
      return new SqlDelightQuery(""
          + "SELECT round(some_real, 1)\n"
          + "FROM some_table",
          new TableSet("some_table"));
    }

    public RowMapper<Long> get_sumMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public RowMapper<Long> get_roundedMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public RowMapper<Double> get_rounded_argMapper() {
      return new RowMapper<Double>() {
        @Override
        public Double map(Cursor cursor) {
          return cursor.getDouble(0);
        }
      };
    }
  }
}
