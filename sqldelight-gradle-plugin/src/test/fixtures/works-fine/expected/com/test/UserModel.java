package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

interface UserModel {
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

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Creator<T> creator;

    private final ColumnAdapter<User.Gender> genderAdapter;

    protected Mapper(Creator<T> creator, ColumnAdapter<User.Gender> genderAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(ID)),
          cursor.getString(cursor.getColumnIndex(FIRST_NAME)),
          cursor.isNull(cursor.getColumnIndex(MIDDLE_INITIAL)) ? null : cursor.getString(cursor.getColumnIndex(MIDDLE_INITIAL)),
          cursor.getString(cursor.getColumnIndex(LAST_NAME)),
          cursor.getInt(cursor.getColumnIndex(AGE)),
          genderAdapter.map(cursor, cursor.getColumnIndex(GENDER))
      );
    }

    public interface Creator<R extends UserModel> {
      R create(long id, String first_name, String middle_initial, String last_name, int age, User.Gender gender);
    }
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender> genderAdapter;

    public UserMarshal(ColumnAdapter<User.Gender> genderAdapter) {
      this.genderAdapter = genderAdapter;
    }

    public UserMarshal(UserModel copy, ColumnAdapter<User.Gender> genderAdapter) {
      this.id(copy.id());
      this.first_name(copy.first_name());
      this.middle_initial(copy.middle_initial());
      this.last_name(copy.last_name());
      this.age(copy.age());
      this.genderAdapter = genderAdapter;
      this.gender(copy.gender());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T id(long id) {
      contentValues.put(ID, id);
      return (T) this;
    }

    public T first_name(String first_name) {
      contentValues.put(FIRST_NAME, first_name);
      return (T) this;
    }

    public T middle_initial(String middle_initial) {
      contentValues.put(MIDDLE_INITIAL, middle_initial);
      return (T) this;
    }

    public T last_name(String last_name) {
      contentValues.put(LAST_NAME, last_name);
      return (T) this;
    }

    public T age(int age) {
      contentValues.put(AGE, age);
      return (T) this;
    }

    public T gender(User.Gender gender) {
      genderAdapter.marshal(contentValues, GENDER, gender);
      return (T) this;
    }
  }
}
