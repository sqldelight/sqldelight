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
  String TABLE_NAME = "user";

  @Deprecated
  String BALANCE = "balance";

  @Deprecated
  String BALANCE_NULLABLE = "balance_nullable";

  String CREATE_TABLE = ""
      + "CREATE TABLE user (\n"
      + "    balance TEXT NOT NULL,\n"
      + "    balance_nullable TEXT NULL\n"
      + ")";

  @NonNull
  User.Money balance();

  @Nullable
  User.Money balance_nullable();

  interface Creator<T extends UserModel> {
    T create(@NonNull User.Money balance, @Nullable User.Money balance_nullable);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(@NonNull Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          userModelFactory.balanceAdapter.decode(cursor.getString(0)),
          cursor.isNull(1) ? null : userModelFactory.balance_nullableAdapter.decode(cursor.getString(1))
      );
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Money, String> balanceAdapter;

    public final ColumnAdapter<User.Money, String> balance_nullableAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<User.Money, String> balanceAdapter,
        @NonNull ColumnAdapter<User.Money, String> balance_nullableAdapter) {
      this.creator = creator;
      this.balanceAdapter = balanceAdapter;
      this.balance_nullableAdapter = balance_nullableAdapter;
    }
  }
}
