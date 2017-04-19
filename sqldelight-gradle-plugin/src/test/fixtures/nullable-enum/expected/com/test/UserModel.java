package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
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

  interface Creator<T extends UserModel> {
    T create(@Nullable User.Gender gender);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.isNull(0) ? null : userModelFactory.genderAdapter.decode(cursor.getString(0))
      );
    }
  }

  final class Marshal {
    final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender, String> genderAdapter;

    Marshal(@Nullable UserModel copy, ColumnAdapter<User.Gender, String> genderAdapter) {
      this.genderAdapter = genderAdapter;
      if (copy != null) {
        this.gender(copy.gender());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal gender(@Nullable User.Gender gender) {
      if (gender != null) {
        contentValues.put("gender", genderAdapter.encode(gender));
      } else {
        contentValues.putNull("gender");
      }
      return this;
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Gender, String> genderAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<User.Gender, String> genderAdapter) {
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
  }
}
