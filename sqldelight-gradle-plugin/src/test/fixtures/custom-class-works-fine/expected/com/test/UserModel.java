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
  String TABLE_NAME = "user";

  String BALANCE = "balance";

  String CREATE_TABLE = ""
      + "CREATE TABLE user (\n"
      + "    balance TEXT NOT NULL\n"
      + ")";

  @NonNull
  User.Money balance();

  interface Creator<T extends UserModel> {
    T create(@NonNull User.Money balance);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          userModelFactory.balanceAdapter.map(cursor, 0)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Money> balanceAdapter;

    Marshal(@Nullable UserModel copy, ColumnAdapter<User.Money> balanceAdapter) {
      this.balanceAdapter = balanceAdapter;
      if (copy != null) {
        this.balance(copy.balance());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal balance(User.Money balance) {
      balanceAdapter.marshal(contentValues, BALANCE, balance);
      return this;
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Money> balanceAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<User.Money> balanceAdapter) {
      this.creator = creator;
      this.balanceAdapter = balanceAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, balanceAdapter);
    }

    public Marshal marshal(UserModel copy) {
      return new Marshal(copy, balanceAdapter);
    }
  }
}
