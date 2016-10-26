package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;

public interface UserModel {
  String TABLE_NAME = "users";

  String ID = "id";

  String FIRST_NAME = "first_name";

  String MIDDLE_INITIAL = "middle_initial";

  String LAST_NAME = "last_name";

  String AGE = "age";

  String GENDER = "gender";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  id INTEGER PRIMARY KEY NOT NULL,\n"
      + "  first_name TEXT NOT NULL,\n"
      + "  middle_initial TEXT,\n"
      + "  last_name TEXT NOT NULL,\n"
      + "  age INTEGER NOT NULL DEFAULT 0,\n"
      + "  gender TEXT NOT NULL\n"
      + ")";

  long id();

  @NonNull
  String first_name();

  @Nullable
  String middle_initial();

  @NonNull
  String last_name();

  int age();

  @NonNull
  Gender gender();

  interface Creator<T extends UserModel> {
    T create(long id, @NonNull String first_name, @Nullable String middle_initial, @NonNull String last_name, int age, @NonNull Gender gender);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.getString(3),
          cursor.getInt(4),
          userModelFactory.genderAdapter.decode(cursor.getString(5))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Gender, String> genderAdapter;

    Marshal(@Nullable UserModel copy, ColumnAdapter<Gender, String> genderAdapter) {
      this.genderAdapter = genderAdapter;
      if (copy != null) {
        this.id(copy.id());
        this.first_name(copy.first_name());
        this.middle_initial(copy.middle_initial());
        this.last_name(copy.last_name());
        this.age(copy.age());
        this.gender(copy.gender());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal id(long id) {
      contentValues.put("id", id);
      return this;
    }

    public Marshal first_name(String first_name) {
      contentValues.put("first_name", first_name);
      return this;
    }

    public Marshal middle_initial(String middle_initial) {
      contentValues.put("middle_initial", middle_initial);
      return this;
    }

    public Marshal last_name(String last_name) {
      contentValues.put("last_name", last_name);
      return this;
    }

    public Marshal age(int age) {
      contentValues.put("age", age);
      return this;
    }

    public Marshal gender(@NonNull Gender gender) {
      contentValues.put("gender", genderAdapter.encode(gender));
      return this;
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Gender, String> genderAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Gender, String> genderAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, genderAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(UserModel copy) {
      return new Marshal(copy, genderAdapter);
    }

    public SqlDelightStatement females() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM users\n"
          + "WHERE gender = 'FEMALE'",
          new String[0], Collections.<String>singleton("users"));
    }

    public Mapper<T> femalesMapper() {
      return new Mapper<T>(this);
    }
  }
}
