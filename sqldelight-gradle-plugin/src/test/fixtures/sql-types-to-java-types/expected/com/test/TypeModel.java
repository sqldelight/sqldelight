package com.test;

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
import java.lang.Short;
import java.lang.String;

public interface TypeModel {
  String TABLE_NAME = "types";

  String I = "i";

  String I_NOT_NULL = "i_not_null";

  String I_AS_BOOL = "i_as_bool";

  String I_AS_BOOL_NOT_NULL = "i_as_bool_not_null";

  String I_AS_INT = "i_as_int";

  String I_AS_INT_NOT_NULL = "i_as_int_not_null";

  String I_AS_SHORT = "i_as_short";

  String I_AS_SHORT_NOT_NULL = "i_as_short_not_null";

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
      + "  i_as_short INTEGER,\n"
      + "  i_as_short_not_null INTEGER NOT NULL,\n"
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
  Short i_as_short();

  short i_as_short_not_null();

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
    T create(@Nullable Long i, long i_not_null, @Nullable Boolean i_as_bool,
        boolean i_as_bool_not_null, @Nullable Integer i_as_int, int i_as_int_not_null,
        @Nullable Short i_as_short, short i_as_short_not_null, @Nullable Long i_as_long,
        long i_as_long_not_null, @Nullable Double i_as_double, double i_as_double_not_null,
        @Nullable CustomType i_as_custom, @NonNull CustomType i_as_custom_not_null,
        @Nullable Double r, double r_not_null, @Nullable Float r_as_float,
        float r_as_float_not_null, @Nullable Double r_as_double, double r_as_double_not_null,
        @Nullable Integer r_as_int, int r_as_int_not_null, @Nullable CustomType r_as_custom,
        @NonNull CustomType r_as_custom_not_null, @Nullable String t, @NonNull String t_not_null,
        @Nullable String t_as_string, @NonNull String t_as_string_not_null,
        @Nullable Long t_as_long, long t_as_long_not_null, @Nullable CustomType t_as_custom,
        @NonNull CustomType t_as_custom_not_null, @Nullable byte[] b, @NonNull byte[] b_not_null,
        @Nullable byte[] b_as_bytes, @NonNull byte[] b_as_bytes_not_null,
        @Nullable String b_as_string, @NonNull String b_as_string_not_null,
        @Nullable CustomType b_as_custom, @NonNull CustomType b_as_custom_not_null);
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
          cursor.isNull(6) ? null : cursor.getShort(6),
          cursor.getShort(7),
          cursor.isNull(8) ? null : cursor.getLong(8),
          cursor.getLong(9),
          cursor.isNull(10) ? null : typeModelFactory.i_as_doubleAdapter.decode(cursor.getLong(10)),
          typeModelFactory.i_as_double_not_nullAdapter.decode(cursor.getLong(11)),
          cursor.isNull(12) ? null : typeModelFactory.i_as_customAdapter.decode(cursor.getLong(12)),
          typeModelFactory.i_as_custom_not_nullAdapter.decode(cursor.getLong(13)),
          cursor.isNull(14) ? null : cursor.getDouble(14),
          cursor.getDouble(15),
          cursor.isNull(16) ? null : cursor.getFloat(16),
          cursor.getFloat(17),
          cursor.isNull(18) ? null : cursor.getDouble(18),
          cursor.getDouble(19),
          cursor.isNull(20) ? null : typeModelFactory.r_as_intAdapter.decode(cursor.getDouble(20)),
          typeModelFactory.r_as_int_not_nullAdapter.decode(cursor.getDouble(21)),
          cursor.isNull(22) ? null : typeModelFactory.r_as_customAdapter.decode(cursor.getDouble(22)),
          typeModelFactory.r_as_custom_not_nullAdapter.decode(cursor.getDouble(23)),
          cursor.isNull(24) ? null : cursor.getString(24),
          cursor.getString(25),
          cursor.isNull(26) ? null : cursor.getString(26),
          cursor.getString(27),
          cursor.isNull(28) ? null : typeModelFactory.t_as_longAdapter.decode(cursor.getString(28)),
          typeModelFactory.t_as_long_not_nullAdapter.decode(cursor.getString(29)),
          cursor.isNull(30) ? null : typeModelFactory.t_as_customAdapter.decode(cursor.getString(30)),
          typeModelFactory.t_as_custom_not_nullAdapter.decode(cursor.getString(31)),
          cursor.isNull(32) ? null : cursor.getBlob(32),
          cursor.getBlob(33),
          cursor.isNull(34) ? null : cursor.getBlob(34),
          cursor.getBlob(35),
          cursor.isNull(36) ? null : typeModelFactory.b_as_stringAdapter.decode(cursor.getBlob(36)),
          typeModelFactory.b_as_string_not_nullAdapter.decode(cursor.getBlob(37)),
          cursor.isNull(38) ? null : typeModelFactory.b_as_customAdapter.decode(cursor.getBlob(38)),
          typeModelFactory.b_as_custom_not_nullAdapter.decode(cursor.getBlob(39))
      );
    }
  }

  final class Factory<T extends TypeModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Double, Long> i_as_doubleAdapter;

    public final ColumnAdapter<Double, Long> i_as_double_not_nullAdapter;

    public final ColumnAdapter<CustomType, Long> i_as_customAdapter;

    public final ColumnAdapter<CustomType, Long> i_as_custom_not_nullAdapter;

    public final ColumnAdapter<Integer, Double> r_as_intAdapter;

    public final ColumnAdapter<Integer, Double> r_as_int_not_nullAdapter;

    public final ColumnAdapter<CustomType, Double> r_as_customAdapter;

    public final ColumnAdapter<CustomType, Double> r_as_custom_not_nullAdapter;

    public final ColumnAdapter<Long, String> t_as_longAdapter;

    public final ColumnAdapter<Long, String> t_as_long_not_nullAdapter;

    public final ColumnAdapter<CustomType, String> t_as_customAdapter;

    public final ColumnAdapter<CustomType, String> t_as_custom_not_nullAdapter;

    public final ColumnAdapter<String, byte[]> b_as_stringAdapter;

    public final ColumnAdapter<String, byte[]> b_as_string_not_nullAdapter;

    public final ColumnAdapter<CustomType, byte[]> b_as_customAdapter;

    public final ColumnAdapter<CustomType, byte[]> b_as_custom_not_nullAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Double, Long> i_as_doubleAdapter,
        ColumnAdapter<Double, Long> i_as_double_not_nullAdapter,
        ColumnAdapter<CustomType, Long> i_as_customAdapter,
        ColumnAdapter<CustomType, Long> i_as_custom_not_nullAdapter,
        ColumnAdapter<Integer, Double> r_as_intAdapter,
        ColumnAdapter<Integer, Double> r_as_int_not_nullAdapter,
        ColumnAdapter<CustomType, Double> r_as_customAdapter,
        ColumnAdapter<CustomType, Double> r_as_custom_not_nullAdapter,
        ColumnAdapter<Long, String> t_as_longAdapter,
        ColumnAdapter<Long, String> t_as_long_not_nullAdapter,
        ColumnAdapter<CustomType, String> t_as_customAdapter,
        ColumnAdapter<CustomType, String> t_as_custom_not_nullAdapter,
        ColumnAdapter<String, byte[]> b_as_stringAdapter,
        ColumnAdapter<String, byte[]> b_as_string_not_nullAdapter,
        ColumnAdapter<CustomType, byte[]> b_as_customAdapter,
        ColumnAdapter<CustomType, byte[]> b_as_custom_not_nullAdapter) {
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
  }
}
