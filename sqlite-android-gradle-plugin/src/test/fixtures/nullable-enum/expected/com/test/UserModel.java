package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import java.lang.String;

public interface UserModel {
  String TABLE_NAME = "users";

  String GENDER = "gender";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  gender TEXT\n"
      + ")";

  @Nullable
  User.Gender gender();

  final class UserMapper<T extends UserModel> {
    private final UserModel.UserMapper.Creator<T> creator;

    protected UserMapper(UserModel.UserMapper.Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(GENDER)) ? null : User.Gender.valueOf(cursor.getString(cursor.getColumnIndex(GENDER)))
      );
    }

    protected interface Creator<R extends UserModel> {
      R create(User.Gender gender);
    }
  }

  class UserMarshal<T extends UserModel.UserMarshal> {
    protected ContentValues contentValues = new ContentValues();

    public UserMarshal() {
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T gender(User.Gender gender) {
      if (gender == null) {
        contentValues.putNull(GENDER);
        return (T) this;
      }
      contentValues.put(GENDER, gender.name());
      return (T) this;
    }
  }
}
