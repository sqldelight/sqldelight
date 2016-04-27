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

  final class Mapper<T extends TypeModel> implements RowMapper<T> {
    private final Creator<T> creator;

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

    protected Mapper(Creator<T> creator, ColumnAdapter<Double> i_as_doubleAdapter, ColumnAdapter<Double> i_as_double_not_nullAdapter, ColumnAdapter<CustomType> i_as_customAdapter, ColumnAdapter<CustomType> i_as_custom_not_nullAdapter, ColumnAdapter<Integer> r_as_intAdapter, ColumnAdapter<Integer> r_as_int_not_nullAdapter, ColumnAdapter<CustomType> r_as_customAdapter, ColumnAdapter<CustomType> r_as_custom_not_nullAdapter, ColumnAdapter<Long> t_as_longAdapter, ColumnAdapter<Long> t_as_long_not_nullAdapter, ColumnAdapter<CustomType> t_as_customAdapter, ColumnAdapter<CustomType> t_as_custom_not_nullAdapter, ColumnAdapter<String> b_as_stringAdapter, ColumnAdapter<String> b_as_string_not_nullAdapter, ColumnAdapter<CustomType> b_as_customAdapter, ColumnAdapter<CustomType> b_as_custom_not_nullAdapter) {
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

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(I)) ? null : cursor.getLong(cursor.getColumnIndex(I)),
          cursor.getLong(cursor.getColumnIndex(I_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(I_AS_BOOL)) ? null : cursor.getInt(cursor.getColumnIndex(I_AS_BOOL)) == 1,
          cursor.getInt(cursor.getColumnIndex(I_AS_BOOL_NOT_NULL)) == 1,
          cursor.isNull(cursor.getColumnIndex(I_AS_INT)) ? null : cursor.getInt(cursor.getColumnIndex(I_AS_INT)),
          cursor.getInt(cursor.getColumnIndex(I_AS_INT_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(I_AS_LONG)) ? null : cursor.getLong(cursor.getColumnIndex(I_AS_LONG)),
          cursor.getLong(cursor.getColumnIndex(I_AS_LONG_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(I_AS_DOUBLE)) ? null : i_as_doubleAdapter.map(cursor, cursor.getColumnIndex(I_AS_DOUBLE)),
          i_as_double_not_nullAdapter.map(cursor, cursor.getColumnIndex(I_AS_DOUBLE_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(I_AS_CUSTOM)) ? null : i_as_customAdapter.map(cursor, cursor.getColumnIndex(I_AS_CUSTOM)),
          i_as_custom_not_nullAdapter.map(cursor, cursor.getColumnIndex(I_AS_CUSTOM_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(R)) ? null : cursor.getDouble(cursor.getColumnIndex(R)),
          cursor.getDouble(cursor.getColumnIndex(R_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(R_AS_FLOAT)) ? null : cursor.getFloat(cursor.getColumnIndex(R_AS_FLOAT)),
          cursor.getFloat(cursor.getColumnIndex(R_AS_FLOAT_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(R_AS_DOUBLE)) ? null : cursor.getDouble(cursor.getColumnIndex(R_AS_DOUBLE)),
          cursor.getDouble(cursor.getColumnIndex(R_AS_DOUBLE_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(R_AS_INT)) ? null : r_as_intAdapter.map(cursor, cursor.getColumnIndex(R_AS_INT)),
          r_as_int_not_nullAdapter.map(cursor, cursor.getColumnIndex(R_AS_INT_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(R_AS_CUSTOM)) ? null : r_as_customAdapter.map(cursor, cursor.getColumnIndex(R_AS_CUSTOM)),
          r_as_custom_not_nullAdapter.map(cursor, cursor.getColumnIndex(R_AS_CUSTOM_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(T)) ? null : cursor.getString(cursor.getColumnIndex(T)),
          cursor.getString(cursor.getColumnIndex(T_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(T_AS_STRING)) ? null : cursor.getString(cursor.getColumnIndex(T_AS_STRING)),
          cursor.getString(cursor.getColumnIndex(T_AS_STRING_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(T_AS_LONG)) ? null : t_as_longAdapter.map(cursor, cursor.getColumnIndex(T_AS_LONG)),
          t_as_long_not_nullAdapter.map(cursor, cursor.getColumnIndex(T_AS_LONG_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(T_AS_CUSTOM)) ? null : t_as_customAdapter.map(cursor, cursor.getColumnIndex(T_AS_CUSTOM)),
          t_as_custom_not_nullAdapter.map(cursor, cursor.getColumnIndex(T_AS_CUSTOM_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(B)) ? null : cursor.getBlob(cursor.getColumnIndex(B)),
          cursor.getBlob(cursor.getColumnIndex(B_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(B_AS_BYTES)) ? null : cursor.getBlob(cursor.getColumnIndex(B_AS_BYTES)),
          cursor.getBlob(cursor.getColumnIndex(B_AS_BYTES_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(B_AS_STRING)) ? null : b_as_stringAdapter.map(cursor, cursor.getColumnIndex(B_AS_STRING)),
          b_as_string_not_nullAdapter.map(cursor, cursor.getColumnIndex(B_AS_STRING_NOT_NULL)),
          cursor.isNull(cursor.getColumnIndex(B_AS_CUSTOM)) ? null : b_as_customAdapter.map(cursor, cursor.getColumnIndex(B_AS_CUSTOM)),
          b_as_custom_not_nullAdapter.map(cursor, cursor.getColumnIndex(B_AS_CUSTOM_NOT_NULL))
      );
    }

    public interface Creator<R extends TypeModel> {
      R create(Long i, long i_not_null, Boolean i_as_bool, boolean i_as_bool_not_null, Integer i_as_int, int i_as_int_not_null, Long i_as_long, long i_as_long_not_null, Double i_as_double, double i_as_double_not_null, CustomType i_as_custom, CustomType i_as_custom_not_null, Double r, double r_not_null, Float r_as_float, float r_as_float_not_null, Double r_as_double, double r_as_double_not_null, Integer r_as_int, int r_as_int_not_null, CustomType r_as_custom, CustomType r_as_custom_not_null, String t, String t_not_null, String t_as_string, String t_as_string_not_null, Long t_as_long, long t_as_long_not_null, CustomType t_as_custom, CustomType t_as_custom_not_null, byte[] b, byte[] b_not_null, byte[] b_as_bytes, byte[] b_as_bytes_not_null, String b_as_string, String b_as_string_not_null, CustomType b_as_custom, CustomType b_as_custom_not_null);
    }
  }

  class TypeMarshal<T extends TypeMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

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

    public TypeMarshal(ColumnAdapter<Double> i_as_doubleAdapter, ColumnAdapter<Double> i_as_double_not_nullAdapter, ColumnAdapter<CustomType> i_as_customAdapter, ColumnAdapter<CustomType> i_as_custom_not_nullAdapter, ColumnAdapter<Integer> r_as_intAdapter, ColumnAdapter<Integer> r_as_int_not_nullAdapter, ColumnAdapter<CustomType> r_as_customAdapter, ColumnAdapter<CustomType> r_as_custom_not_nullAdapter, ColumnAdapter<Long> t_as_longAdapter, ColumnAdapter<Long> t_as_long_not_nullAdapter, ColumnAdapter<CustomType> t_as_customAdapter, ColumnAdapter<CustomType> t_as_custom_not_nullAdapter, ColumnAdapter<String> b_as_stringAdapter, ColumnAdapter<String> b_as_string_not_nullAdapter, ColumnAdapter<CustomType> b_as_customAdapter, ColumnAdapter<CustomType> b_as_custom_not_nullAdapter) {
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

    public TypeMarshal(TypeModel copy, ColumnAdapter<Double> i_as_doubleAdapter, ColumnAdapter<Double> i_as_double_not_nullAdapter, ColumnAdapter<CustomType> i_as_customAdapter, ColumnAdapter<CustomType> i_as_custom_not_nullAdapter, ColumnAdapter<Integer> r_as_intAdapter, ColumnAdapter<Integer> r_as_int_not_nullAdapter, ColumnAdapter<CustomType> r_as_customAdapter, ColumnAdapter<CustomType> r_as_custom_not_nullAdapter, ColumnAdapter<Long> t_as_longAdapter, ColumnAdapter<Long> t_as_long_not_nullAdapter, ColumnAdapter<CustomType> t_as_customAdapter, ColumnAdapter<CustomType> t_as_custom_not_nullAdapter, ColumnAdapter<String> b_as_stringAdapter, ColumnAdapter<String> b_as_string_not_nullAdapter, ColumnAdapter<CustomType> b_as_customAdapter, ColumnAdapter<CustomType> b_as_custom_not_nullAdapter) {
      this.i(copy.i());
      this.i_not_null(copy.i_not_null());
      this.i_as_bool(copy.i_as_bool());
      this.i_as_bool_not_null(copy.i_as_bool_not_null());
      this.i_as_int(copy.i_as_int());
      this.i_as_int_not_null(copy.i_as_int_not_null());
      this.i_as_long(copy.i_as_long());
      this.i_as_long_not_null(copy.i_as_long_not_null());
      this.i_as_doubleAdapter = i_as_doubleAdapter;
      this.i_as_double(copy.i_as_double());
      this.i_as_double_not_nullAdapter = i_as_double_not_nullAdapter;
      this.i_as_double_not_null(copy.i_as_double_not_null());
      this.i_as_customAdapter = i_as_customAdapter;
      this.i_as_custom(copy.i_as_custom());
      this.i_as_custom_not_nullAdapter = i_as_custom_not_nullAdapter;
      this.i_as_custom_not_null(copy.i_as_custom_not_null());
      this.r(copy.r());
      this.r_not_null(copy.r_not_null());
      this.r_as_float(copy.r_as_float());
      this.r_as_float_not_null(copy.r_as_float_not_null());
      this.r_as_double(copy.r_as_double());
      this.r_as_double_not_null(copy.r_as_double_not_null());
      this.r_as_intAdapter = r_as_intAdapter;
      this.r_as_int(copy.r_as_int());
      this.r_as_int_not_nullAdapter = r_as_int_not_nullAdapter;
      this.r_as_int_not_null(copy.r_as_int_not_null());
      this.r_as_customAdapter = r_as_customAdapter;
      this.r_as_custom(copy.r_as_custom());
      this.r_as_custom_not_nullAdapter = r_as_custom_not_nullAdapter;
      this.r_as_custom_not_null(copy.r_as_custom_not_null());
      this.t(copy.t());
      this.t_not_null(copy.t_not_null());
      this.t_as_string(copy.t_as_string());
      this.t_as_string_not_null(copy.t_as_string_not_null());
      this.t_as_longAdapter = t_as_longAdapter;
      this.t_as_long(copy.t_as_long());
      this.t_as_long_not_nullAdapter = t_as_long_not_nullAdapter;
      this.t_as_long_not_null(copy.t_as_long_not_null());
      this.t_as_customAdapter = t_as_customAdapter;
      this.t_as_custom(copy.t_as_custom());
      this.t_as_custom_not_nullAdapter = t_as_custom_not_nullAdapter;
      this.t_as_custom_not_null(copy.t_as_custom_not_null());
      this.b(copy.b());
      this.b_not_null(copy.b_not_null());
      this.b_as_bytes(copy.b_as_bytes());
      this.b_as_bytes_not_null(copy.b_as_bytes_not_null());
      this.b_as_stringAdapter = b_as_stringAdapter;
      this.b_as_string(copy.b_as_string());
      this.b_as_string_not_nullAdapter = b_as_string_not_nullAdapter;
      this.b_as_string_not_null(copy.b_as_string_not_null());
      this.b_as_customAdapter = b_as_customAdapter;
      this.b_as_custom(copy.b_as_custom());
      this.b_as_custom_not_nullAdapter = b_as_custom_not_nullAdapter;
      this.b_as_custom_not_null(copy.b_as_custom_not_null());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T i(Long i) {
      contentValues.put(I, i);
      return (T) this;
    }

    public T i_not_null(long i_not_null) {
      contentValues.put(I_NOT_NULL, i_not_null);
      return (T) this;
    }

    public T i_as_bool(Boolean i_as_bool) {
      if (i_as_bool == null) {
        contentValues.putNull(I_AS_BOOL);
        return (T) this;
      }
      contentValues.put(I_AS_BOOL, i_as_bool ? 1 : 0);
      return (T) this;
    }

    public T i_as_bool_not_null(boolean i_as_bool_not_null) {
      contentValues.put(I_AS_BOOL_NOT_NULL, i_as_bool_not_null ? 1 : 0);
      return (T) this;
    }

    public T i_as_int(Integer i_as_int) {
      contentValues.put(I_AS_INT, i_as_int);
      return (T) this;
    }

    public T i_as_int_not_null(int i_as_int_not_null) {
      contentValues.put(I_AS_INT_NOT_NULL, i_as_int_not_null);
      return (T) this;
    }

    public T i_as_long(Long i_as_long) {
      contentValues.put(I_AS_LONG, i_as_long);
      return (T) this;
    }

    public T i_as_long_not_null(long i_as_long_not_null) {
      contentValues.put(I_AS_LONG_NOT_NULL, i_as_long_not_null);
      return (T) this;
    }

    public T i_as_double(Double i_as_double) {
      i_as_doubleAdapter.marshal(contentValues, I_AS_DOUBLE, i_as_double);
      return (T) this;
    }

    public T i_as_double_not_null(double i_as_double_not_null) {
      i_as_double_not_nullAdapter.marshal(contentValues, I_AS_DOUBLE_NOT_NULL, i_as_double_not_null);
      return (T) this;
    }

    public T i_as_custom(CustomType i_as_custom) {
      i_as_customAdapter.marshal(contentValues, I_AS_CUSTOM, i_as_custom);
      return (T) this;
    }

    public T i_as_custom_not_null(CustomType i_as_custom_not_null) {
      i_as_custom_not_nullAdapter.marshal(contentValues, I_AS_CUSTOM_NOT_NULL, i_as_custom_not_null);
      return (T) this;
    }

    public T r(Double r) {
      contentValues.put(R, r);
      return (T) this;
    }

    public T r_not_null(double r_not_null) {
      contentValues.put(R_NOT_NULL, r_not_null);
      return (T) this;
    }

    public T r_as_float(Float r_as_float) {
      contentValues.put(R_AS_FLOAT, r_as_float);
      return (T) this;
    }

    public T r_as_float_not_null(float r_as_float_not_null) {
      contentValues.put(R_AS_FLOAT_NOT_NULL, r_as_float_not_null);
      return (T) this;
    }

    public T r_as_double(Double r_as_double) {
      contentValues.put(R_AS_DOUBLE, r_as_double);
      return (T) this;
    }

    public T r_as_double_not_null(double r_as_double_not_null) {
      contentValues.put(R_AS_DOUBLE_NOT_NULL, r_as_double_not_null);
      return (T) this;
    }

    public T r_as_int(Integer r_as_int) {
      r_as_intAdapter.marshal(contentValues, R_AS_INT, r_as_int);
      return (T) this;
    }

    public T r_as_int_not_null(int r_as_int_not_null) {
      r_as_int_not_nullAdapter.marshal(contentValues, R_AS_INT_NOT_NULL, r_as_int_not_null);
      return (T) this;
    }

    public T r_as_custom(CustomType r_as_custom) {
      r_as_customAdapter.marshal(contentValues, R_AS_CUSTOM, r_as_custom);
      return (T) this;
    }

    public T r_as_custom_not_null(CustomType r_as_custom_not_null) {
      r_as_custom_not_nullAdapter.marshal(contentValues, R_AS_CUSTOM_NOT_NULL, r_as_custom_not_null);
      return (T) this;
    }

    public T t(String t) {
      contentValues.put(T, t);
      return (T) this;
    }

    public T t_not_null(String t_not_null) {
      contentValues.put(T_NOT_NULL, t_not_null);
      return (T) this;
    }

    public T t_as_string(String t_as_string) {
      contentValues.put(T_AS_STRING, t_as_string);
      return (T) this;
    }

    public T t_as_string_not_null(String t_as_string_not_null) {
      contentValues.put(T_AS_STRING_NOT_NULL, t_as_string_not_null);
      return (T) this;
    }

    public T t_as_long(Long t_as_long) {
      t_as_longAdapter.marshal(contentValues, T_AS_LONG, t_as_long);
      return (T) this;
    }

    public T t_as_long_not_null(long t_as_long_not_null) {
      t_as_long_not_nullAdapter.marshal(contentValues, T_AS_LONG_NOT_NULL, t_as_long_not_null);
      return (T) this;
    }

    public T t_as_custom(CustomType t_as_custom) {
      t_as_customAdapter.marshal(contentValues, T_AS_CUSTOM, t_as_custom);
      return (T) this;
    }

    public T t_as_custom_not_null(CustomType t_as_custom_not_null) {
      t_as_custom_not_nullAdapter.marshal(contentValues, T_AS_CUSTOM_NOT_NULL, t_as_custom_not_null);
      return (T) this;
    }

    public T b(byte[] b) {
      contentValues.put(B, b);
      return (T) this;
    }

    public T b_not_null(byte[] b_not_null) {
      contentValues.put(B_NOT_NULL, b_not_null);
      return (T) this;
    }

    public T b_as_bytes(byte[] b_as_bytes) {
      contentValues.put(B_AS_BYTES, b_as_bytes);
      return (T) this;
    }

    public T b_as_bytes_not_null(byte[] b_as_bytes_not_null) {
      contentValues.put(B_AS_BYTES_NOT_NULL, b_as_bytes_not_null);
      return (T) this;
    }

    public T b_as_string(String b_as_string) {
      b_as_stringAdapter.marshal(contentValues, B_AS_STRING, b_as_string);
      return (T) this;
    }

    public T b_as_string_not_null(String b_as_string_not_null) {
      b_as_string_not_nullAdapter.marshal(contentValues, B_AS_STRING_NOT_NULL, b_as_string_not_null);
      return (T) this;
    }

    public T b_as_custom(CustomType b_as_custom) {
      b_as_customAdapter.marshal(contentValues, B_AS_CUSTOM, b_as_custom);
      return (T) this;
    }

    public T b_as_custom_not_null(CustomType b_as_custom_not_null) {
      b_as_custom_not_nullAdapter.marshal(contentValues, B_AS_CUSTOM_NOT_NULL, b_as_custom_not_null);
      return (T) this;
    }
  }
}
