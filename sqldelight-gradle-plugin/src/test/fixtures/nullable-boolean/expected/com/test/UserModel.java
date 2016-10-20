package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Boolean;
import java.lang.Override;
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

  interface Creator<T extends UserModel> {
    T create(@Nullable Boolean tall);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getInt(0) == 1
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable UserModel copy) {
      if (copy != null) {
        this.tall(copy.tall());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal tall(Boolean tall) {
      if (tall == null) {
        contentValues.putNull("tall");
        return this;
      }
      contentValues.put("tall", tall ? 1 : 0);
      return this;
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(UserModel copy) {
      return new Marshal(copy);
    }
  }
}
