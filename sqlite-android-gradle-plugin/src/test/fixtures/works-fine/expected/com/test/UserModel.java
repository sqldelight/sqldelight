package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
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
      + "    FROM users\n"
      + "   WHERE gender = 'FEMALE'";

  long id();

  String firstName();

  @Nullable
  String middleInitial();

  String lastName();

  int age();

  User.Gender gender();

  final class UserMapper<T extends UserModel> {
    private final UserModel.UserMapper.Creator<T> creator;

    protected UserMapper(UserModel.UserMapper.Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor) {
      return creator.create(
        cursor.getLong(cursor.getColumnIndex(ID)),
        cursor.getString(cursor.getColumnIndex(FIRST_NAME)),
        cursor.isNull(cursor.getColumnIndex(MIDDLE_INITIAL)) ? null : cursor.getString(cursor.getColumnIndex(MIDDLE_INITIAL)),
        cursor.getString(cursor.getColumnIndex(LAST_NAME)),
        cursor.getInt(cursor.getColumnIndex(AGE)),
        User.Gender.valueOf(cursor.getString(cursor.getColumnIndex(GENDER))));}

    protected interface Creator<R extends UserModel> {
      R create(long id, String firstName, String middleInitial, String lastName, int age, User.Gender gender);
    }
  }

  class UserMarshal<T extends UserModel.UserMarshal> {
    protected ContentValues contentValues = new ContentValues();

    public UserMarshal() {
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T id(long id) {
      contentValues.put(ID, id);
      return (T) this;
    }

    public T firstName(String firstName) {
      contentValues.put(FIRST_NAME, firstName);
      return (T) this;
    }

    public T middleInitial(String middleInitial) {
      contentValues.put(MIDDLE_INITIAL, middleInitial);
      return (T) this;
    }

    public T lastName(String lastName) {
      contentValues.put(LAST_NAME, lastName);
      return (T) this;
    }

    public T age(int age) {
      contentValues.put(AGE, age);
      return (T) this;
    }

    public T gender(User.Gender gender) {
      contentValues.put(GENDER, gender.name());
      return (T) this;
    }
  }
}
