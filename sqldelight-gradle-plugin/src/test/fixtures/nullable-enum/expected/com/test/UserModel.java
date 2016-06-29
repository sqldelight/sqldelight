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
          cursor.isNull(0) ? null : userModelFactory.genderAdapter.map(cursor, 0)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender> genderAdapter;

    Marshal(@Nullable UserModel copy, ColumnAdapter<User.Gender> genderAdapter) {
      this.genderAdapter = genderAdapter;
      if (copy != null) {
        this.gender(copy.gender());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
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
  }
}
