package com.test;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface UserModel {
  @Deprecated
  String TABLE_NAME = "users";

  @Deprecated
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

    public Mapper(@NonNull Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.isNull(0) ? null : userModelFactory.genderAdapter.decode(cursor.getString(0))
      );
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Gender, String> genderAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<User.Gender, String> genderAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
    }
  }
}
