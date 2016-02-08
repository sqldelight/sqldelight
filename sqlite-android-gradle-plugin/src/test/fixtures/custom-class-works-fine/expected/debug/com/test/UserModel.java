package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.String;

public interface UserModel {
  String TABLE_NAME = "user";

  String BALANCE = "balance";

  String CREATE_TABLE = ""
      + "CREATE TABLE user (\n"
      + "    balance BLOB NOT NULL\n"
      + ")";

  User.Money balance();

  final class Mapper<T extends UserModel> {
    private final UserModel.Mapper.Creator<T> creator;

    private final UserModel.Mapper.BalanceMapper balanceMapper;

    protected Mapper(UserModel.Mapper.Creator<T> creator, UserModel.Mapper.BalanceMapper balanceMapper) {
      this.creator = creator;
      this.balanceMapper = balanceMapper;
    }

    public T map(Cursor cursor) {
      return creator.create(
          balanceMapper.map(cursor, cursor.getColumnIndex(BALANCE))
      );
    }

    protected interface Creator<R extends UserModel> {
      R create(User.Money balance);
    }

    protected interface BalanceMapper {
      User.Money map(Cursor cursor, int columnIndex);
    }
  }

  class UserMarshal<T extends UserModel.UserMarshal> {
    protected ContentValues contentValues = new ContentValues();

    private final UserModel.UserMarshal.BalanceMarshal balanceMarshal;

    public UserMarshal(UserModel.UserMarshal.BalanceMarshal balanceMarshal) {
      this.balanceMarshal = balanceMarshal;
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T balance(User.Money balance) {
      balanceMarshal.marshal(contentValues, BALANCE, balance);
      return (T) this;
    }

    protected interface BalanceMarshal {
      void marshal(ContentValues contentValues, String columnName, User.Money balance);
    }
  }
}
