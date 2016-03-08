package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
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

  final class Mapper<T extends UserModel> {
    private final Creator<T> creator;

    private final ColumnAdapter<User.Gender> genderAdapter;

    protected Mapper(Creator<T> creator, ColumnAdapter<User.Gender> genderAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(GENDER)) ? null : genderAdapter.map(cursor, cursor.getColumnIndex(GENDER))
      );
    }

    public interface Creator<R extends UserModel> {
      R create(User.Gender gender);
    }
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender> genderAdapter;

    public UserMarshal(ColumnAdapter<User.Gender> genderAdapter) {
      this.genderAdapter = genderAdapter;
    }

    public UserMarshal(UserModel copy, ColumnAdapter<User.Gender> genderAdapter) {
      this.genderAdapter = genderAdapter;
      this.gender(copy.gender());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T gender(User.Gender gender) {
      genderAdapter.marshal(contentValues, GENDER, gender);
      return (T) this;
    }
  }
}
