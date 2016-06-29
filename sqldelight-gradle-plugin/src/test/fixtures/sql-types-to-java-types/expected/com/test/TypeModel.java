package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TypeModel {
  String TABLE_NAME = "types";

  String I = "i";

  String I_NOT_NULL = "i_not_null";

  String I_AS_BOOL = "i_as_bool";

  String I_AS_BOOL_NOT_NULL = "i_as_bool_not_null";

  String I_AS_INT = "i_as_int";

  String I_AS_INT_NOT_NULL = "i_as_int_not_null";

  String I_AS_LONG = "i_as_long";

  String I_AS_LONG_NOT_NULL = "i_as_long_not_null";

  String I_AS_DOUBLE = "i_as_double";

  String I_AS_DOUBLE_NOT_NULL = "i_as_double_not_null";

  String I_AS_CUSTOM = "i_as_custom";

  String I_AS_CUSTOM_NOT_NULL = "i_as_custom_not_null";

  String R = "r";

  String R_NOT_NULL = "r_not_null";

  String R_AS_FLOAT = "r_as_float";

  String R_AS_FLOAT_NOT_NULL = "r_as_float_not_null";

  String R_AS_DOUBLE = "r_as_double";

  String R_AS_DOUBLE_NOT_NULL = "r_as_double_not_null";

  String R_AS_INT = "r_as_int";

  String R_AS_INT_NOT_NULL = "r_as_int_not_null";

  String R_AS_CUSTOM = "r_as_custom";

  String R_AS_CUSTOM_NOT_NULL = "r_as_custom_not_null";

  String T = "t";

  String T_NOT_NULL = "t_not_null";

  String T_AS_STRING = "t_as_string";

  String T_AS_STRING_NOT_NULL = "t_as_string_not_null";

  String T_AS_LONG = "t_as_long";

  String T_AS_LONG_NOT_NULL = "t_as_long_not_null";

  String T_AS_CUSTOM = "t_as_custom";

  String T_AS_CUSTOM_NOT_NULL = "t_as_custom_not_null";

  String B = "b";

  String B_NOT_NULL = "b_not_null";

  String B_AS_BYTES = "b_as_bytes";

  String B_AS_BYTES_NOT_NULL = "b_as_bytes_not_null";

  String B_AS_STRING = "b_as_string";

  String B_AS_STRING_NOT_NULL = "b_as_string_not_null";

  String B_AS_CUSTOM = "b_as_custom";

  String B_AS_CUSTOM_NOT_NULL = "b_as_custom_not_null";

  String CREATE_TABLE = ""
      + "CREATE TABLE types (\n"
      + "  -- Implicit Java type\n"
      + "  i INTEGER,\n"
      + "  i_not_null INTEGER NOT NULL,\n"
      + "  -- Explicit, handled Java types\n"
      + "  i_as_bool INTEGER,\n"
      + "  i_as_bool_not_null INTEGER NOT NULL,\n"
      + "  i_as_int INTEGER,\n"
      + "  i_as_int_not_null INTEGER NOT NULL,\n"
      + "  i_as_long INTEGER,\n"
      + "  i_as_long_not_null INTEGER NOT NULL,\n"
      + "  -- Explicit, non-handled Java type\n"
      + "  i_as_double INTEGER,\n"
      + "  i_as_double_not_null INTEGER NOT NULL,\n"
      + "  -- Explicit, non-handled custom type\n"
      + "  i_as_custom INTEGER,\n"
      + "  i_as_custom_not_null INTEGER NOT NULL,\n"
      + "\n"
      + "  -- Implicit Java type\n"
      + "  r REAL,\n"
      + "  r_not_null REAL NOT NULL,\n"
      + "  -- Explicit, handled Java type\n"
      + "  r_as_float REAL,\n"
      + "  r_as_float_not_null REAL NOT NULL,\n"
      + "  r_as_double REAL,\n"
      + "  r_as_double_not_null REAL NOT NULL,\n"
      + "  -- Explicit, non-handled Java type\n"
      + "  r_as_int REAL,\n"
      + "  r_as_int_not_null REAL NOT NULL,\n"
      + "  -- Explicit, non-handled custom type\n"
      + "  r_as_custom REAL,\n"
      + "  r_as_custom_not_null REAL NOT NULL,\n"
      + "\n"
      + "  -- Implicit Java type\n"
      + "  t TEXT,\n"
      + "  t_not_null TEXT NOT NULL,\n"
      + "  -- Explicit, handled Java type\n"
      + "  t_as_string TEXT,\n"
      + "  t_as_string_not_null TEXT NOT NULL,\n"
      + "  -- Explicit, non-handled Java type\n"
      + "  t_as_long TEXT,\n"
      + "  t_as_long_not_null TEXT NOT NULL,\n"
      + "  -- Explicit, non-handled custom type\n"
      + "  t_as_custom TEXT,\n"
      + "  t_as_custom_not_null TEXT NOT NULL,\n"
      + "\n"
      + "  -- Implicit Java type\n"
      + "  b BLOB,\n"
      + "  b_not_null BLOB NOT NULL,\n"
      + "  -- Explicit, handled Java type\n"
      + "  b_as_bytes BLOB,\n"
      + "  b_as_bytes_not_null BLOB NOT NULL,\n"
      + "  -- Explicit, non-handled Java type\n"
      + "  b_as_string BLOB,\n"
      + "  b_as_string_not_null BLOB NOT NULL,\n"
      + "  -- Explicit, non-handled custom type\n"
      + "  b_as_custom BLOB,\n"
      + "  b_as_custom_not_null BLOB NOT NULL\n"
      + ")";

  @Nullable
  Long i();

  long i_not_null();

  @Nullable
  Boolean i_as_bool();

  boolean i_as_bool_not_null();

  @Nullable
  Integer i_as_int();

  int i_as_int_not_null();

  @Nullable
  Long i_as_long();

  long i_as_long_not_null();

  @Nullable
  Double i_as_double();

  double i_as_double_not_null();

  @Nullable
  CustomType i_as_custom();

  @NonNull
  CustomType i_as_custom_not_null();

  @Nullable
  Double r();

  double r_not_null();

  @Nullable
  Float r_as_float();

  float r_as_float_not_null();

  @Nullable
  Double r_as_double();

  double r_as_double_not_null();

  @Nullable
  Integer r_as_int();

  int r_as_int_not_null();

  @Nullable
  CustomType r_as_custom();

  @NonNull
  CustomType r_as_custom_not_null();

  @Nullable
  String t();

  @NonNull
  String t_not_null();

  @Nullable
  String t_as_string();

  @NonNull
  String t_as_string_not_null();

  @Nullable
  Long t_as_long();

  long t_as_long_not_null();

  @Nullable
  CustomType t_as_custom();

  @NonNull
  CustomType t_as_custom_not_null();

  @Nullable
  byte[] b();

  @NonNull
  byte[] b_not_null();

  @Nullable
  byte[] b_as_bytes();

  @NonNull
  byte[] b_as_bytes_not_null();

  @Nullable
  String b_as_string();

  @NonNull
  String b_as_string_not_null();

  @Nullable
  CustomType b_as_custom();

  @NonNull
  CustomType b_as_custom_not_null();

  interface Creator<T extends TypeModel> {
    T create(@Nullable Long i, long i_not_null, @Nullable Boolean i_as_bool, boolean i_as_bool_not_null, @Nullable Integer i_as_int, int i_as_int_not_null, @Nullable Long i_as_long, long i_as_long_not_null, @Nullable Double i_as_double, double i_as_double_not_null, @Nullable CustomType i_as_custom, @NonNull CustomType i_as_custom_not_null, @Nullable Double r, double r_not_null, @Nullable Float r_as_float, float r_as_float_not_null, @Nullable Double r_as_double, double r_as_double_not_null, @Nullable Integer r_as_int, int r_as_int_not_null, @Nullable CustomType r_as_custom, @NonNull CustomType r_as_custom_not_null, @Nullable String t, @NonNull String t_not_null, @Nullable String t_as_string, @NonNull String t_as_string_not_null, @Nullable Long t_as_long, long t_as_long_not_null, @Nullable CustomType t_as_custom, @NonNull CustomType t_as_custom_not_null, @Nullable byte[] b, @NonNull byte[] b_not_null, @Nullable byte[] b_as_bytes, @NonNull byte[] b_as_bytes_not_null, @Nullable String b_as_string, @NonNull String b_as_string_not_null, @Nullable CustomType b_as_custom, @NonNull CustomType b_as_custom_not_null);
  }

  final class Mapper<T extends TypeModel> implements RowMapper<T> {
    private final Factory<T> typeModelFactory;

    public Mapper(Factory<T> typeModelFactory) {
      this.typeModelFactory = typeModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return typeModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getLong(1),
          cursor.isNull(2) ? null : cursor.getInt(2) == 1,
          cursor.getInt(3) == 1,
          cursor.isNull(4) ? null : cursor.getInt(4),
          cursor.getInt(5),
          cursor.isNull(6) ? null : cursor.getLong(6),
          cursor.getLong(7),
          cursor.isNull(8) ? null : typeModelFactory.i_as_doubleAdapter.map(cursor, 8),
          typeModelFactory.i_as_double_not_nullAdapter.map(cursor, 9),
          cursor.isNull(10) ? null : typeModelFactory.i_as_customAdapter.map(cursor, 10),
          typeModelFactory.i_as_custom_not_nullAdapter.map(cursor, 11),
          cursor.isNull(12) ? null : cursor.getDouble(12),
          cursor.getDouble(13),
          cursor.isNull(14) ? null : cursor.getFloat(14),
          cursor.getFloat(15),
          cursor.isNull(16) ? null : cursor.getDouble(16),
          cursor.getDouble(17),
          cursor.isNull(18) ? null : typeModelFactory.r_as_intAdapter.map(cursor, 18),
          typeModelFactory.r_as_int_not_nullAdapter.map(cursor, 19),
          cursor.isNull(20) ? null : typeModelFactory.r_as_customAdapter.map(cursor, 20),
          typeModelFactory.r_as_custom_not_nullAdapter.map(cursor, 21),
          cursor.isNull(22) ? null : cursor.getString(22),
          cursor.getString(23),
          cursor.isNull(24) ? null : cursor.getString(24),
          cursor.getString(25),
          cursor.isNull(26) ? null : typeModelFactory.t_as_longAdapter.map(cursor, 26),
          typeModelFactory.t_as_long_not_nullAdapter.map(cursor, 27),
          cursor.isNull(28) ? null : typeModelFactory.t_as_customAdapter.map(cursor, 28),
          typeModelFactory.t_as_custom_not_nullAdapter.map(cursor, 29),
          cursor.isNull(30) ? null : cursor.getBlob(30),
          cursor.getBlob(31),
          cursor.isNull(32) ? null : cursor.getBlob(32),
          cursor.getBlob(33),
          cursor.isNull(34) ? null : typeModelFactory.b_as_stringAdapter.map(cursor, 34),
          typeModelFactory.b_as_string_not_nullAdapter.map(cursor, 35),
          cursor.isNull(36) ? null : typeModelFactory.b_as_customAdapter.map(cursor, 36),
          typeModelFactory.b_as_custom_not_nullAdapter.map(cursor, 37)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Double> i_as_doubleAdapter;

    private final ColumnAdapter<Double> i_as_double_not_nullAdapter;

    private final ColumnAdapter<CustomType> i_as_customAdapter;

    private final ColumnAdapter<CustomType> i_as_custom_not_nullAdapter;

    private final ColumnAdapter<Integer> r_as_intAdapter;

    private final ColumnAdapter<Integer> r_as_int_not_nullAdapter;

    private final ColumnAdapter<CustomType> r_as_customAdapter;

    private final ColumnAdapter<CustomType> r_as_custom_not_nullAdapter;

    private final ColumnAdapter<Long> t_as_longAdapter;

    private final ColumnAdapter<Long> t_as_long_not_nullAdapter;

    private final ColumnAdapter<CustomType> t_as_customAdapter;

    private final ColumnAdapter<CustomType> t_as_custom_not_nullAdapter;

    private final ColumnAdapter<String> b_as_stringAdapter;

    private final ColumnAdapter<String> b_as_string_not_nullAdapter;

    private final ColumnAdapter<CustomType> b_as_customAdapter;

    private final ColumnAdapter<CustomType> b_as_custom_not_nullAdapter;

    Marshal(@Nullable TypeModel copy, ColumnAdapter<Double> i_as_doubleAdapter, ColumnAdapter<Double> i_as_double_not_nullAdapter, ColumnAdapter<CustomType> i_as_customAdapter, ColumnAdapter<CustomType> i_as_custom_not_nullAdapter, ColumnAdapter<Integer> r_as_intAdapter, ColumnAdapter<Integer> r_as_int_not_nullAdapter, ColumnAdapter<CustomType> r_as_customAdapter, ColumnAdapter<CustomType> r_as_custom_not_nullAdapter, ColumnAdapter<Long> t_as_longAdapter, ColumnAdapter<Long> t_as_long_not_nullAdapter, ColumnAdapter<CustomType> t_as_customAdapter, ColumnAdapter<CustomType> t_as_custom_not_nullAdapter, ColumnAdapter<String> b_as_stringAdapter, ColumnAdapter<String> b_as_string_not_nullAdapter, ColumnAdapter<CustomType> b_as_customAdapter, ColumnAdapter<CustomType> b_as_custom_not_nullAdapter) {
      this.i_as_doubleAdapter = i_as_doubleAdapter;
      this.i_as_double_not_nullAdapter = i_as_double_not_nullAdapter;
      this.i_as_customAdapter = i_as_customAdapter;
      this.i_as_custom_not_nullAdapter = i_as_custom_not_nullAdapter;
      this.r_as_intAdapter = r_as_intAdapter;
      this.r_as_int_not_nullAdapter = r_as_int_not_nullAdapter;
      this.r_as_customAdapter = r_as_customAdapter;
      this.r_as_custom_not_nullAdapter = r_as_custom_not_nullAdapter;
      this.t_as_longAdapter = t_as_longAdapter;
      this.t_as_long_not_nullAdapter = t_as_long_not_nullAdapter;
      this.t_as_customAdapter = t_as_customAdapter;
      this.t_as_custom_not_nullAdapter = t_as_custom_not_nullAdapter;
      this.b_as_stringAdapter = b_as_stringAdapter;
      this.b_as_string_not_nullAdapter = b_as_string_not_nullAdapter;
      this.b_as_customAdapter = b_as_customAdapter;
      this.b_as_custom_not_nullAdapter = b_as_custom_not_nullAdapter;
      if (copy != null) {
        this.i(copy.i());
        this.i_not_null(copy.i_not_null());
        this.i_as_bool(copy.i_as_bool());
        this.i_as_bool_not_null(copy.i_as_bool_not_null());
        this.i_as_int(copy.i_as_int());
        this.i_as_int_not_null(copy.i_as_int_not_null());
        this.i_as_long(copy.i_as_long());
        this.i_as_long_not_null(copy.i_as_long_not_null());
        this.i_as_double(copy.i_as_double());
        this.i_as_double_not_null(copy.i_as_double_not_null());
        this.i_as_custom(copy.i_as_custom());
        this.i_as_custom_not_null(copy.i_as_custom_not_null());
        this.r(copy.r());
        this.r_not_null(copy.r_not_null());
        this.r_as_float(copy.r_as_float());
        this.r_as_float_not_null(copy.r_as_float_not_null());
        this.r_as_double(copy.r_as_double());
        this.r_as_double_not_null(copy.r_as_double_not_null());
        this.r_as_int(copy.r_as_int());
        this.r_as_int_not_null(copy.r_as_int_not_null());
        this.r_as_custom(copy.r_as_custom());
        this.r_as_custom_not_null(copy.r_as_custom_not_null());
        this.t(copy.t());
        this.t_not_null(copy.t_not_null());
        this.t_as_string(copy.t_as_string());
        this.t_as_string_not_null(copy.t_as_string_not_null());
        this.t_as_long(copy.t_as_long());
        this.t_as_long_not_null(copy.t_as_long_not_null());
        this.t_as_custom(copy.t_as_custom());
        this.t_as_custom_not_null(copy.t_as_custom_not_null());
        this.b(copy.b());
        this.b_not_null(copy.b_not_null());
        this.b_as_bytes(copy.b_as_bytes());
        this.b_as_bytes_not_null(copy.b_as_bytes_not_null());
        this.b_as_string(copy.b_as_string());
        this.b_as_string_not_null(copy.b_as_string_not_null());
        this.b_as_custom(copy.b_as_custom());
        this.b_as_custom_not_null(copy.b_as_custom_not_null());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal i(Long i) {
      contentValues.put(I, i);
      return this;
    }

    public Marshal i_not_null(long i_not_null) {
      contentValues.put(I_NOT_NULL, i_not_null);
      return this;
    }

    public Marshal i_as_bool(Boolean i_as_bool) {
      if (i_as_bool == null) {
        contentValues.putNull(I_AS_BOOL);
        return this;
      }
      contentValues.put(I_AS_BOOL, i_as_bool ? 1 : 0);
      return this;
    }

    public Marshal i_as_bool_not_null(boolean i_as_bool_not_null) {
      contentValues.put(I_AS_BOOL_NOT_NULL, i_as_bool_not_null ? 1 : 0);
      return this;
    }

    public Marshal i_as_int(Integer i_as_int) {
      contentValues.put(I_AS_INT, i_as_int);
      return this;
    }

    public Marshal i_as_int_not_null(int i_as_int_not_null) {
      contentValues.put(I_AS_INT_NOT_NULL, i_as_int_not_null);
      return this;
    }

    public Marshal i_as_long(Long i_as_long) {
      contentValues.put(I_AS_LONG, i_as_long);
      return this;
    }

    public Marshal i_as_long_not_null(long i_as_long_not_null) {
      contentValues.put(I_AS_LONG_NOT_NULL, i_as_long_not_null);
      return this;
    }

    public Marshal i_as_double(Double i_as_double) {
      i_as_doubleAdapter.marshal(contentValues, I_AS_DOUBLE, i_as_double);
      return this;
    }

    public Marshal i_as_double_not_null(double i_as_double_not_null) {
      i_as_double_not_nullAdapter.marshal(contentValues, I_AS_DOUBLE_NOT_NULL, i_as_double_not_null);
      return this;
    }

    public Marshal i_as_custom(CustomType i_as_custom) {
      i_as_customAdapter.marshal(contentValues, I_AS_CUSTOM, i_as_custom);
      return this;
    }

    public Marshal i_as_custom_not_null(CustomType i_as_custom_not_null) {
      i_as_custom_not_nullAdapter.marshal(contentValues, I_AS_CUSTOM_NOT_NULL, i_as_custom_not_null);
      return this;
    }

    public Marshal r(Double r) {
      contentValues.put(R, r);
      return this;
    }

    public Marshal r_not_null(double r_not_null) {
      contentValues.put(R_NOT_NULL, r_not_null);
      return this;
    }

    public Marshal r_as_float(Float r_as_float) {
      contentValues.put(R_AS_FLOAT, r_as_float);
      return this;
    }

    public Marshal r_as_float_not_null(float r_as_float_not_null) {
      contentValues.put(R_AS_FLOAT_NOT_NULL, r_as_float_not_null);
      return this;
    }

    public Marshal r_as_double(Double r_as_double) {
      contentValues.put(R_AS_DOUBLE, r_as_double);
      return this;
    }

    public Marshal r_as_double_not_null(double r_as_double_not_null) {
      contentValues.put(R_AS_DOUBLE_NOT_NULL, r_as_double_not_null);
      return this;
    }

    public Marshal r_as_int(Integer r_as_int) {
      r_as_intAdapter.marshal(contentValues, R_AS_INT, r_as_int);
      return this;
    }

    public Marshal r_as_int_not_null(int r_as_int_not_null) {
      r_as_int_not_nullAdapter.marshal(contentValues, R_AS_INT_NOT_NULL, r_as_int_not_null);
      return this;
    }

    public Marshal r_as_custom(CustomType r_as_custom) {
      r_as_customAdapter.marshal(contentValues, R_AS_CUSTOM, r_as_custom);
      return this;
    }

    public Marshal r_as_custom_not_null(CustomType r_as_custom_not_null) {
      r_as_custom_not_nullAdapter.marshal(contentValues, R_AS_CUSTOM_NOT_NULL, r_as_custom_not_null);
      return this;
    }

    public Marshal t(String t) {
      contentValues.put(T, t);
      return this;
    }

    public Marshal t_not_null(String t_not_null) {
      contentValues.put(T_NOT_NULL, t_not_null);
      return this;
    }

    public Marshal t_as_string(String t_as_string) {
      contentValues.put(T_AS_STRING, t_as_string);
      return this;
    }

    public Marshal t_as_string_not_null(String t_as_string_not_null) {
      contentValues.put(T_AS_STRING_NOT_NULL, t_as_string_not_null);
      return this;
    }

    public Marshal t_as_long(Long t_as_long) {
      t_as_longAdapter.marshal(contentValues, T_AS_LONG, t_as_long);
      return this;
    }

    public Marshal t_as_long_not_null(long t_as_long_not_null) {
      t_as_long_not_nullAdapter.marshal(contentValues, T_AS_LONG_NOT_NULL, t_as_long_not_null);
      return this;
    }

    public Marshal t_as_custom(CustomType t_as_custom) {
      t_as_customAdapter.marshal(contentValues, T_AS_CUSTOM, t_as_custom);
      return this;
    }

    public Marshal t_as_custom_not_null(CustomType t_as_custom_not_null) {
      t_as_custom_not_nullAdapter.marshal(contentValues, T_AS_CUSTOM_NOT_NULL, t_as_custom_not_null);
      return this;
    }

    public Marshal b(byte[] b) {
      contentValues.put(B, b);
      return this;
    }

    public Marshal b_not_null(byte[] b_not_null) {
      contentValues.put(B_NOT_NULL, b_not_null);
      return this;
    }

    public Marshal b_as_bytes(byte[] b_as_bytes) {
      contentValues.put(B_AS_BYTES, b_as_bytes);
      return this;
    }

    public Marshal b_as_bytes_not_null(byte[] b_as_bytes_not_null) {
      contentValues.put(B_AS_BYTES_NOT_NULL, b_as_bytes_not_null);
      return this;
    }

    public Marshal b_as_string(String b_as_string) {
      b_as_stringAdapter.marshal(contentValues, B_AS_STRING, b_as_string);
      return this;
    }

    public Marshal b_as_string_not_null(String b_as_string_not_null) {
      b_as_string_not_nullAdapter.marshal(contentValues, B_AS_STRING_NOT_NULL, b_as_string_not_null);
      return this;
    }

    public Marshal b_as_custom(CustomType b_as_custom) {
      b_as_customAdapter.marshal(contentValues, B_AS_CUSTOM, b_as_custom);
      return this;
    }

    public Marshal b_as_custom_not_null(CustomType b_as_custom_not_null) {
      b_as_custom_not_nullAdapter.marshal(contentValues, B_AS_CUSTOM_NOT_NULL, b_as_custom_not_null);
      return this;
    }
  }

  final class Factory<T extends TypeModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Double> i_as_doubleAdapter;

    public final ColumnAdapter<Double> i_as_double_not_nullAdapter;

    public final ColumnAdapter<CustomType> i_as_customAdapter;

    public final ColumnAdapter<CustomType> i_as_custom_not_nullAdapter;

    public final ColumnAdapter<Integer> r_as_intAdapter;

    public final ColumnAdapter<Integer> r_as_int_not_nullAdapter;

    public final ColumnAdapter<CustomType> r_as_customAdapter;

    public final ColumnAdapter<CustomType> r_as_custom_not_nullAdapter;

    public final ColumnAdapter<Long> t_as_longAdapter;

    public final ColumnAdapter<Long> t_as_long_not_nullAdapter;

    public final ColumnAdapter<CustomType> t_as_customAdapter;

    public final ColumnAdapter<CustomType> t_as_custom_not_nullAdapter;

    public final ColumnAdapter<String> b_as_stringAdapter;

    public final ColumnAdapter<String> b_as_string_not_nullAdapter;

    public final ColumnAdapter<CustomType> b_as_customAdapter;

    public final ColumnAdapter<CustomType> b_as_custom_not_nullAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Double> i_as_doubleAdapter, ColumnAdapter<Double> i_as_double_not_nullAdapter, ColumnAdapter<CustomType> i_as_customAdapter, ColumnAdapter<CustomType> i_as_custom_not_nullAdapter, ColumnAdapter<Integer> r_as_intAdapter, ColumnAdapter<Integer> r_as_int_not_nullAdapter, ColumnAdapter<CustomType> r_as_customAdapter, ColumnAdapter<CustomType> r_as_custom_not_nullAdapter, ColumnAdapter<Long> t_as_longAdapter, ColumnAdapter<Long> t_as_long_not_nullAdapter, ColumnAdapter<CustomType> t_as_customAdapter, ColumnAdapter<CustomType> t_as_custom_not_nullAdapter, ColumnAdapter<String> b_as_stringAdapter, ColumnAdapter<String> b_as_string_not_nullAdapter, ColumnAdapter<CustomType> b_as_customAdapter, ColumnAdapter<CustomType> b_as_custom_not_nullAdapter) {
      this.creator = creator;
      this.i_as_doubleAdapter = i_as_doubleAdapter;
      this.i_as_double_not_nullAdapter = i_as_double_not_nullAdapter;
      this.i_as_customAdapter = i_as_customAdapter;
      this.i_as_custom_not_nullAdapter = i_as_custom_not_nullAdapter;
      this.r_as_intAdapter = r_as_intAdapter;
      this.r_as_int_not_nullAdapter = r_as_int_not_nullAdapter;
      this.r_as_customAdapter = r_as_customAdapter;
      this.r_as_custom_not_nullAdapter = r_as_custom_not_nullAdapter;
      this.t_as_longAdapter = t_as_longAdapter;
      this.t_as_long_not_nullAdapter = t_as_long_not_nullAdapter;
      this.t_as_customAdapter = t_as_customAdapter;
      this.t_as_custom_not_nullAdapter = t_as_custom_not_nullAdapter;
      this.b_as_stringAdapter = b_as_stringAdapter;
      this.b_as_string_not_nullAdapter = b_as_string_not_nullAdapter;
      this.b_as_customAdapter = b_as_customAdapter;
      this.b_as_custom_not_nullAdapter = b_as_custom_not_nullAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, i_as_doubleAdapter, i_as_double_not_nullAdapter, i_as_customAdapter, i_as_custom_not_nullAdapter, r_as_intAdapter, r_as_int_not_nullAdapter, r_as_customAdapter, r_as_custom_not_nullAdapter, t_as_longAdapter, t_as_long_not_nullAdapter, t_as_customAdapter, t_as_custom_not_nullAdapter, b_as_stringAdapter, b_as_string_not_nullAdapter, b_as_customAdapter, b_as_custom_not_nullAdapter);
    }

    public Marshal marshal(TypeModel copy) {
      return new Marshal(copy, i_as_doubleAdapter, i_as_double_not_nullAdapter, i_as_customAdapter, i_as_custom_not_nullAdapter, r_as_intAdapter, r_as_int_not_nullAdapter, r_as_customAdapter, r_as_custom_not_nullAdapter, t_as_longAdapter, t_as_long_not_nullAdapter, t_as_customAdapter, t_as_custom_not_nullAdapter, b_as_stringAdapter, b_as_string_not_nullAdapter, b_as_customAdapter, b_as_custom_not_nullAdapter);
    }
  }
}
