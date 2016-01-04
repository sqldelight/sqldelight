package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import java.lang.Boolean;
import java.lang.String;

public interface UserModel {
  String TABLE_NAME = "users";

  String TALL = "tall";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  tall INTEGER\n"
      + ")";

  @Nullable
  Boolean tall();

  final class UserMapper<T extends UserModel> {
    private final UserModel.UserMapper.Creator<T> creator;

    protected UserMapper(UserModel.UserMapper.Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(TALL)) ? null : cursor.getInt(cursor.getColumnIndex(TALL)) == 1
      );
    }

    protected interface Creator<R extends UserModel> {
      R create(Boolean tall);
    }
  }

  class UserMarshal<T extends UserModel.UserMarshal> {
    protected ContentValues contentValues = new ContentValues();

    public UserMarshal() {
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T tall(Boolean tall) {
      if (tall == null) {
        contentValues.putNull(TALL);
        return (T) this;
      }
      contentValues.put(TALL, tall ? 1 : 0);
      return (T) this;
    }
  }
}
