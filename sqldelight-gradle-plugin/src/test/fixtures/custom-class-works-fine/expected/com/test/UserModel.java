package com.test;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface UserModel {
  String TABLE_NAME = "user";

  String BALANCE = "balance";

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

    public Mapper(Factory<T> userModelFactory) {
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

    public Factory(Creator<T> creator, ColumnAdapter<User.Money, String> balanceAdapter,
        ColumnAdapter<User.Money, String> balance_nullableAdapter) {
      this.creator = creator;
      this.balanceAdapter = balanceAdapter;
      this.balance_nullableAdapter = balance_nullableAdapter;
    }
  }
}
