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

  final class Mapper<T extends UserModel> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(TALL)) ? null : cursor.getInt(cursor.getColumnIndex(TALL)) == 1
      );
    }

    public interface Creator<R extends UserModel> {
      R create(Boolean tall);
    }
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public UserMarshal() {
    }

    public UserMarshal(UserModel copy) {
      this.tall(copy.tall());
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
