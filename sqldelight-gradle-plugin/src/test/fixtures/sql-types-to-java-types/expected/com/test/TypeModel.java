package com.test;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
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

    public SqlDelightQuery all_select(@Nullable Long i, long i_not_null,
        @Nullable Boolean i_as_bool, boolean i_as_bool_not_null, @Nullable Integer i_as_int,
        int i_as_int_not_null, @Nullable Short i_as_short, short i_as_short_not_null,
        @Nullable Long i_as_long, long i_as_long_not_null, @Nullable Double i_as_double,
        double i_as_double_not_null, @Nullable CustomType i_as_custom,
        @NonNull CustomType i_as_custom_not_null, @Nullable Double r, double r_not_null,
        @Nullable Float r_as_float, float r_as_float_not_null, @Nullable Double r_as_double,
        double r_as_double_not_null, @Nullable Integer r_as_int, int r_as_int_not_null,
        @Nullable CustomType r_as_custom, @NonNull CustomType r_as_custom_not_null,
        @Nullable String t, @NonNull String t_not_null, @Nullable String t_as_string,
        @NonNull String t_as_string_not_null, @Nullable Long t_as_long, long t_as_long_not_null,
        @Nullable CustomType t_as_custom, @NonNull CustomType t_as_custom_not_null,
        @Nullable byte[] b, @NonNull byte[] b_not_null, @Nullable byte[] b_as_bytes,
        @NonNull byte[] b_as_bytes_not_null, @Nullable String b_as_string,
        @NonNull String b_as_string_not_null, @Nullable CustomType b_as_custom,
        @NonNull CustomType b_as_custom_not_null) {
      return new All_selectQuery(i, i_not_null, i_as_bool, i_as_bool_not_null, i_as_int,
          i_as_int_not_null, i_as_short, i_as_short_not_null, i_as_long, i_as_long_not_null,
          i_as_double, i_as_double_not_null, i_as_custom, i_as_custom_not_null, r, r_not_null,
          r_as_float, r_as_float_not_null, r_as_double, r_as_double_not_null, r_as_int,
          r_as_int_not_null, r_as_custom, r_as_custom_not_null, t, t_not_null, t_as_string,
          t_as_string_not_null, t_as_long, t_as_long_not_null, t_as_custom, t_as_custom_not_null, b,
          b_not_null, b_as_bytes, b_as_bytes_not_null, b_as_string, b_as_string_not_null,
          b_as_custom, b_as_custom_not_null);
    }

    public Mapper<T> all_selectMapper() {
      return new Mapper<T>(this);
    }

    private final class All_selectQuery extends SqlDelightQuery {
      @Nullable
      private final Long i;

      private final long i_not_null;

      @Nullable
      private final Boolean i_as_bool;

      private final boolean i_as_bool_not_null;

      @Nullable
      private final Integer i_as_int;

      private final int i_as_int_not_null;

      @Nullable
      private final Short i_as_short;

      private final short i_as_short_not_null;

      @Nullable
      private final Long i_as_long;

      private final long i_as_long_not_null;

      @Nullable
      private final Double i_as_double;

      private final double i_as_double_not_null;

      @Nullable
      private final CustomType i_as_custom;

      @NonNull
      private final CustomType i_as_custom_not_null;

      @Nullable
      private final Double r;

      private final double r_not_null;

      @Nullable
      private final Float r_as_float;

      private final float r_as_float_not_null;

      @Nullable
      private final Double r_as_double;

      private final double r_as_double_not_null;

      @Nullable
      private final Integer r_as_int;

      private final int r_as_int_not_null;

      @Nullable
      private final CustomType r_as_custom;

      @NonNull
      private final CustomType r_as_custom_not_null;

      @Nullable
      private final String t;

      @NonNull
      private final String t_not_null;

      @Nullable
      private final String t_as_string;

      @NonNull
      private final String t_as_string_not_null;

      @Nullable
      private final Long t_as_long;

      private final long t_as_long_not_null;

      @Nullable
      private final CustomType t_as_custom;

      @NonNull
      private final CustomType t_as_custom_not_null;

      @Nullable
      private final byte[] b;

      @NonNull
      private final byte[] b_not_null;

      @Nullable
      private final byte[] b_as_bytes;

      @NonNull
      private final byte[] b_as_bytes_not_null;

      @Nullable
      private final String b_as_string;

      @NonNull
      private final String b_as_string_not_null;

      @Nullable
      private final CustomType b_as_custom;

      @NonNull
      private final CustomType b_as_custom_not_null;

      All_selectQuery(@Nullable Long i, long i_not_null, @Nullable Boolean i_as_bool,
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
          @Nullable CustomType b_as_custom, @NonNull CustomType b_as_custom_not_null) {
        super("SELECT *\n"
            + "FROM types\n"
            + "WHERE i = ?1\n"
            + "   OR i_not_null = ?2\n"
            + "   OR i_as_bool = ?3\n"
            + "   OR i_as_bool_not_null = ?4\n"
            + "   OR i_as_int = ?5\n"
            + "   OR i_as_int_not_null = ?6\n"
            + "   OR i_as_short = ?7\n"
            + "   OR i_as_short_not_null = ?8\n"
            + "   OR i_as_long = ?9\n"
            + "   OR i_as_long_not_null = ?10\n"
            + "   OR i_as_double = ?11\n"
            + "   OR i_as_double_not_null = ?12\n"
            + "   OR i_as_custom = ?13\n"
            + "   OR i_as_custom_not_null = ?14\n"
            + "   OR r = ?15\n"
            + "   OR r_not_null = ?16\n"
            + "   OR r_as_float = ?17\n"
            + "   OR r_as_float_not_null = ?18\n"
            + "   OR r_as_double = ?19\n"
            + "   OR r_as_double_not_null = ?20\n"
            + "   OR r_as_int = ?21\n"
            + "   OR r_as_int_not_null = ?22\n"
            + "   OR r_as_custom = ?23\n"
            + "   OR r_as_custom_not_null = ?24\n"
            + "   OR t = ?25\n"
            + "   OR t_not_null = ?26\n"
            + "   OR t_as_string = ?27\n"
            + "   OR t_as_string_not_null = ?28\n"
            + "   OR t_as_long = ?29\n"
            + "   OR t_as_long_not_null = ?30\n"
            + "   OR t_as_custom = ?31\n"
            + "   OR t_as_custom_not_null = ?32\n"
            + "   OR b = ?33\n"
            + "   OR b_not_null = ?34\n"
            + "   OR b_as_bytes = ?35\n"
            + "   OR b_as_bytes_not_null = ?36\n"
            + "   OR b_as_string = ?37\n"
            + "   OR b_as_string_not_null = ?38\n"
            + "   OR b_as_custom = ?39\n"
            + "   OR b_as_custom_not_null = ?40",
            new TableSet("types"));

        this.i = i;
        this.i_not_null = i_not_null;
        this.i_as_bool = i_as_bool;
        this.i_as_bool_not_null = i_as_bool_not_null;
        this.i_as_int = i_as_int;
        this.i_as_int_not_null = i_as_int_not_null;
        this.i_as_short = i_as_short;
        this.i_as_short_not_null = i_as_short_not_null;
        this.i_as_long = i_as_long;
        this.i_as_long_not_null = i_as_long_not_null;
        this.i_as_double = i_as_double;
        this.i_as_double_not_null = i_as_double_not_null;
        this.i_as_custom = i_as_custom;
        this.i_as_custom_not_null = i_as_custom_not_null;
        this.r = r;
        this.r_not_null = r_not_null;
        this.r_as_float = r_as_float;
        this.r_as_float_not_null = r_as_float_not_null;
        this.r_as_double = r_as_double;
        this.r_as_double_not_null = r_as_double_not_null;
        this.r_as_int = r_as_int;
        this.r_as_int_not_null = r_as_int_not_null;
        this.r_as_custom = r_as_custom;
        this.r_as_custom_not_null = r_as_custom_not_null;
        this.t = t;
        this.t_not_null = t_not_null;
        this.t_as_string = t_as_string;
        this.t_as_string_not_null = t_as_string_not_null;
        this.t_as_long = t_as_long;
        this.t_as_long_not_null = t_as_long_not_null;
        this.t_as_custom = t_as_custom;
        this.t_as_custom_not_null = t_as_custom_not_null;
        this.b = b;
        this.b_not_null = b_not_null;
        this.b_as_bytes = b_as_bytes;
        this.b_as_bytes_not_null = b_as_bytes_not_null;
        this.b_as_string = b_as_string;
        this.b_as_string_not_null = b_as_string_not_null;
        this.b_as_custom = b_as_custom;
        this.b_as_custom_not_null = b_as_custom_not_null;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Long i = this.i;
        if (i != null) {
          program.bindLong(1, i);
        } else {
          program.bindNull(1);
        }

        program.bindLong(2, i_not_null);

        Boolean i_as_bool = this.i_as_bool;
        if (i_as_bool != null) {
          program.bindLong(3, i_as_bool ? 1 : 0);
        } else {
          program.bindNull(3);
        }

        program.bindLong(4, i_as_bool_not_null ? 1 : 0);

        Integer i_as_int = this.i_as_int;
        if (i_as_int != null) {
          program.bindLong(5, i_as_int);
        } else {
          program.bindNull(5);
        }

        program.bindLong(6, i_as_int_not_null);

        Short i_as_short = this.i_as_short;
        if (i_as_short != null) {
          program.bindLong(7, i_as_short);
        } else {
          program.bindNull(7);
        }

        program.bindLong(8, i_as_short_not_null);

        Long i_as_long = this.i_as_long;
        if (i_as_long != null) {
          program.bindLong(9, i_as_long);
        } else {
          program.bindNull(9);
        }

        program.bindLong(10, i_as_long_not_null);

        Double i_as_double = this.i_as_double;
        if (i_as_double != null) {
          program.bindLong(11, i_as_doubleAdapter.encode(i_as_double));
        } else {
          program.bindNull(11);
        }

        program.bindLong(12, i_as_double_not_nullAdapter.encode(i_as_double_not_null));

        CustomType i_as_custom = this.i_as_custom;
        if (i_as_custom != null) {
          program.bindLong(13, i_as_customAdapter.encode(i_as_custom));
        } else {
          program.bindNull(13);
        }

        program.bindLong(14, i_as_custom_not_nullAdapter.encode(i_as_custom_not_null));

        Double r = this.r;
        if (r != null) {
          program.bindDouble(15, r);
        } else {
          program.bindNull(15);
        }

        program.bindDouble(16, r_not_null);

        Float r_as_float = this.r_as_float;
        if (r_as_float != null) {
          program.bindDouble(17, r_as_float);
        } else {
          program.bindNull(17);
        }

        program.bindDouble(18, r_as_float_not_null);

        Double r_as_double = this.r_as_double;
        if (r_as_double != null) {
          program.bindDouble(19, r_as_double);
        } else {
          program.bindNull(19);
        }

        program.bindDouble(20, r_as_double_not_null);

        Integer r_as_int = this.r_as_int;
        if (r_as_int != null) {
          program.bindDouble(21, r_as_intAdapter.encode(r_as_int));
        } else {
          program.bindNull(21);
        }

        program.bindDouble(22, r_as_int_not_nullAdapter.encode(r_as_int_not_null));

        CustomType r_as_custom = this.r_as_custom;
        if (r_as_custom != null) {
          program.bindDouble(23, r_as_customAdapter.encode(r_as_custom));
        } else {
          program.bindNull(23);
        }

        program.bindDouble(24, r_as_custom_not_nullAdapter.encode(r_as_custom_not_null));

        String t = this.t;
        if (t != null) {
          program.bindString(25, t);
        } else {
          program.bindNull(25);
        }

        program.bindString(26, t_not_null);

        String t_as_string = this.t_as_string;
        if (t_as_string != null) {
          program.bindString(27, t_as_string);
        } else {
          program.bindNull(27);
        }

        program.bindString(28, t_as_string_not_null);

        Long t_as_long = this.t_as_long;
        if (t_as_long != null) {
          program.bindString(29, t_as_longAdapter.encode(t_as_long));
        } else {
          program.bindNull(29);
        }

        program.bindString(30, t_as_long_not_nullAdapter.encode(t_as_long_not_null));

        CustomType t_as_custom = this.t_as_custom;
        if (t_as_custom != null) {
          program.bindString(31, t_as_customAdapter.encode(t_as_custom));
        } else {
          program.bindNull(31);
        }

        program.bindString(32, t_as_custom_not_nullAdapter.encode(t_as_custom_not_null));

        byte[] b = this.b;
        if (b != null) {
          program.bindBlob(33, b);
        } else {
          program.bindNull(33);
        }

        program.bindBlob(34, b_not_null);

        byte[] b_as_bytes = this.b_as_bytes;
        if (b_as_bytes != null) {
          program.bindBlob(35, b_as_bytes);
        } else {
          program.bindNull(35);
        }

        program.bindBlob(36, b_as_bytes_not_null);

        String b_as_string = this.b_as_string;
        if (b_as_string != null) {
          program.bindBlob(37, b_as_stringAdapter.encode(b_as_string));
        } else {
          program.bindNull(37);
        }

        program.bindBlob(38, b_as_string_not_nullAdapter.encode(b_as_string_not_null));

        CustomType b_as_custom = this.b_as_custom;
        if (b_as_custom != null) {
          program.bindBlob(39, b_as_customAdapter.encode(b_as_custom));
        } else {
          program.bindNull(39);
        }

        program.bindBlob(40, b_as_custom_not_nullAdapter.encode(b_as_custom_not_null));
      }
    }
  }
}
