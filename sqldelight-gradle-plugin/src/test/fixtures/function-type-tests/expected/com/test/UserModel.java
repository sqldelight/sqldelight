package com.test;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface UserModel {
  @Deprecated
  String TABLE_NAME = "users";

  @Deprecated
  String ID = "id";

  @Deprecated
  String AGE = "age";

  @Deprecated
  String GENDER = "gender";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  id INTEGER PRIMARY KEY NOT NULL,\n"
      + "  age INTEGER NOT NULL DEFAULT 0,\n"
      + "  gender TEXT NOT NULL\n"
      + ")";

  long id();

  int age();

  @NonNull
  String gender();

  interface SelectWithFunctionsModel {
    @NonNull
    String gender();

    @NonNull
    String date();

    @NonNull
    String time();

    @NonNull
    String datetime();

    @NonNull
    String strftime();

    @NonNull
    String char_();

    @NonNull
    String hex();

    @NonNull
    String quote();

    @NonNull
    String soundex();

    @NonNull
    String sqlite_compileoption_get();

    @NonNull
    String sqlite_source_id();

    @NonNull
    String sqlite_version();

    @NonNull
    String typeof();

    @NonNull
    String lower_age();

    @NonNull
    String ltrim_gender();

    @NonNull
    String printf();

    @NonNull
    String replace();

    @NonNull
    String rtrim_gender();

    @NonNull
    String substr();

    @NonNull
    String trim_gender();

    @NonNull
    String upper_gender();

    @NonNull
    String group_concat_age();

    long changes();

    long last_insert_rowid();

    long random();

    long sqlite_compileoption_used();

    long total_changes();

    long count();

    long instr();

    long length_gender();

    long unicode_gender();

    @NonNull
    byte[] randomblob();

    @NonNull
    byte[] zeroblob();

    double total_age();

    double avg_age();

    long round_avg_age();

    long sum_age();

    long abs_age();

    @NonNull
    String likeihood_gender();

    long likely_int_literal();

    long unlikely_int_literal();

    long coalesce_age();

    long ifnull();

    @Nullable
    Long nullif();

    long max_age();

    long min_age();
  }

  interface SelectWithFunctionsCreator<T extends SelectWithFunctionsModel> {
    T create(@NonNull String gender, @NonNull String date, @NonNull String time,
        @NonNull String datetime, @NonNull String strftime, @NonNull String char_,
        @NonNull String hex, @NonNull String quote, @NonNull String soundex,
        @NonNull String sqlite_compileoption_get, @NonNull String sqlite_source_id,
        @NonNull String sqlite_version, @NonNull String typeof, @NonNull String lower_age,
        @NonNull String ltrim_gender, @NonNull String printf, @NonNull String replace,
        @NonNull String rtrim_gender, @NonNull String substr, @NonNull String trim_gender,
        @NonNull String upper_gender, @NonNull String group_concat_age, long changes,
        long last_insert_rowid, long random, long sqlite_compileoption_used, long total_changes,
        long count, long instr, long length_gender, long unicode_gender, @NonNull byte[] randomblob,
        @NonNull byte[] zeroblob, double total_age, double avg_age, long round_avg_age,
        long sum_age, long abs_age, @NonNull String likeihood_gender, long likely_int_literal,
        long unlikely_int_literal, long coalesce_age, long ifnull, @Nullable Long nullif,
        long max_age, long min_age);
  }

  final class SelectWithFunctionsMapper<T extends SelectWithFunctionsModel> implements RowMapper<T> {
    private final SelectWithFunctionsCreator<T> creator;

    public SelectWithFunctionsMapper(SelectWithFunctionsCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.getString(1),
          cursor.getString(2),
          cursor.getString(3),
          cursor.getString(4),
          cursor.getString(5),
          cursor.getString(6),
          cursor.getString(7),
          cursor.getString(8),
          cursor.getString(9),
          cursor.getString(10),
          cursor.getString(11),
          cursor.getString(12),
          cursor.getString(13),
          cursor.getString(14),
          cursor.getString(15),
          cursor.getString(16),
          cursor.getString(17),
          cursor.getString(18),
          cursor.getString(19),
          cursor.getString(20),
          cursor.getString(21),
          cursor.getLong(22),
          cursor.getLong(23),
          cursor.getLong(24),
          cursor.getLong(25),
          cursor.getLong(26),
          cursor.getLong(27),
          cursor.getLong(28),
          cursor.getLong(29),
          cursor.getLong(30),
          cursor.getBlob(31),
          cursor.getBlob(32),
          cursor.getDouble(33),
          cursor.getDouble(34),
          cursor.getLong(35),
          cursor.getLong(36),
          cursor.getLong(37),
          cursor.getString(38),
          cursor.getLong(39),
          cursor.getLong(40),
          cursor.getLong(41),
          cursor.getLong(42),
          cursor.isNull(43) ? null : cursor.getLong(43),
          cursor.getLong(44),
          cursor.getLong(45)
      );
    }
  }

  interface Creator<T extends UserModel> {
    T create(long id, int age, @NonNull String gender);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(@NonNull Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.getString(2)
      );
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery selectWithFunctions() {
      return new SqlDelightQuery(""
          + "SELECT\n"
          + "  gender,\n"
          + "  date('%y-%m-%d') as date,\n"
          + "  time('%h:%m') as time,\n"
          + "  datetime('%y-%m-%d %h:%m') as datetime,\n"
          + "  strftime('%m', '2014-10-07 02:34:56') as strftime,\n"
          + "  char('m') as char,\n"
          + "  hex('d') as hex,\n"
          + "  quote('42') as quote,\n"
          + "  soundex('x') as soundex,\n"
          + "  sqlite_compileoption_get(1) as sqlite_compileoption_get,\n"
          + "  sqlite_source_id() as sqlite_source_id,\n"
          + "  sqlite_version() as sqlite_version,\n"
          + "  typeof(123) as typeof,\n"
          + "  lower(age) as lower_age,\n"
          + "  ltrim(gender) as ltrim_gender,\n"
          + "  printf('%d', 123) as printf,\n"
          + "  replace('hello world', 'world', 'universe') as replace,\n"
          + "  rtrim(gender) as rtrim_gender,\n"
          + "  substr('abcdefg', 2) as substr,\n"
          + "  trim(gender) as trim_gender,\n"
          + "  upper(gender) as upper_gender,\n"
          + "  group_concat(age) as group_concat_age,\n"
          + "  changes() as changes,\n"
          + "  last_insert_rowid() as last_insert_rowid,\n"
          + "  random() as random,\n"
          + "  sqlite_compileoption_used('someoption') as sqlite_compileoption_used,\n"
          + "  total_changes() as total_changes,\n"
          + "  count(*) as count,\n"
          + "  instr('hello world', 'world') as instr,\n"
          + "  length(gender) as length_gender,\n"
          + "  unicode(gender) as unicode_gender,\n"
          + "  randomblob(1) as randomblob,\n"
          + "  zeroblob(1) as zeroblob,\n"
          + "  total(age) as total_age,\n"
          + "  avg(age) as avg_age,\n"
          + "  round(avg(age)) as round_avg_age,\n"
          + "  sum(age) as sum_age,\n"
          + "  abs(age) as abs_age,\n"
          + "  likelihood(gender, 0.5) as likeihood_gender,\n"
          + "  likely(1),\n"
          + "  unlikely(0),\n"
          + "  coalesce(null, age) as coalesce_age,\n"
          + "  ifnull(null, 1) as ifnull,\n"
          + "  nullif(1, null) as nullif,\n"
          + "  max(age) as max_age,\n"
          + "  min(age) as min_age\n"
          + "FROM users\n"
          + "GROUP BY gender",
          new TableSet("users"));
    }

    @NonNull
    public <R extends SelectWithFunctionsModel> SelectWithFunctionsMapper<R> selectWithFunctionsMapper(
        SelectWithFunctionsCreator<R> creator) {
      return new SelectWithFunctionsMapper<R>(creator);
    }
  }
}
