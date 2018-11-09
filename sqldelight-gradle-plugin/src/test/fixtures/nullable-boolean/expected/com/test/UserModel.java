package com.test;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Boolean;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface UserModel {
  @Deprecated
  String TABLE_NAME = "users";

  @Deprecated
  String TALL = "tall";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  tall INTEGER\n"
      + ")";

  @Nullable
  Boolean tall();

  interface Creator<T extends UserModel> {
    T create(@Nullable Boolean tall);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(@NonNull Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getInt(0) == 1
      );
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }
}
