package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Double;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "some_table";

  String _ID = "_id";

  String QUANTITY = "quantity";

  String SOME_REAL = "some_real";

  String CREATE_TABLE = ""
      + "CREATE TABLE some_table (\n"
      + "    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "    quantity INTEGER NOT NULL DEFAULT 0,\n"
      + "    some_real REAL NOT NULL DEFAULT 1.0\n"
      + ")";

  String GET_SUM = ""
      + "SELECT sum(quantity)\n"
      + "FROM some_table";

  String GET_ROUNDED = ""
      + "SELECT round(some_real)\n"
      + "FROM some_table";

  String GET_ROUNDED_ARG = ""
      + "SELECT round(some_real, 1)\n"
      + "FROM some_table";

  long _id();

  int quantity();

  double some_real();

  interface Creator<T extends TestModel> {
    T create(long _id, int quantity, double some_real);
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
          cursor.getInt(1),
          cursor.getDouble(2)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.quantity(copy.quantity());
        this.some_real(copy.some_real());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal quantity(int quantity) {
      contentValues.put("quantity", quantity);
      return this;
    }

    public Marshal some_real(double some_real) {
      contentValues.put("some_real", some_real);
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
