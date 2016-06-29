package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

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

  String FEMALES = ""
      + "SELECT *\n"
      + "FROM users\n"
      + "WHERE gender = 'FEMALE'";

  long id();

  @NonNull
  String first_name();

  @Nullable
  String middle_initial();

  @NonNull
  String last_name();

  int age();

  @NonNull
  User.Gender gender();

  interface Creator<T extends UserModel> {
    T create(long id, @NonNull String first_name, @Nullable String middle_initial, @NonNull String last_name, int age, @NonNull User.Gender gender);
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
          userModelFactory.genderAdapter.map(cursor, 5)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender> genderAdapter;

    Marshal(@Nullable UserModel copy, ColumnAdapter<User.Gender> genderAdapter) {
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
      contentValues.put(ID, id);
      return this;
    }

    public Marshal first_name(String first_name) {
      contentValues.put(FIRST_NAME, first_name);
      return this;
    }

    public Marshal middle_initial(String middle_initial) {
      contentValues.put(MIDDLE_INITIAL, middle_initial);
      return this;
    }

    public Marshal last_name(String last_name) {
      contentValues.put(LAST_NAME, last_name);
      return this;
    }

    public Marshal age(int age) {
      contentValues.put(AGE, age);
      return this;
    }

    public Marshal gender(User.Gender gender) {
      genderAdapter.marshal(contentValues, GENDER, gender);
      return this;
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Gender> genderAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<User.Gender> genderAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, genderAdapter);
    }

    public Marshal marshal(UserModel copy) {
      return new Marshal(copy, genderAdapter);
    }

    public Mapper<T> femalesMapper() {
      return new Mapper<T>(this);
    }
  }
}
