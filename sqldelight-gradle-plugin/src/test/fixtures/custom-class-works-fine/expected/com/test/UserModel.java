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
    private final Creator<T> creator;

    private final BalanceMapper balanceMapper;

    protected Mapper(Creator<T> creator, BalanceMapper balanceMapper) {
      this.creator = creator;
      this.balanceMapper = balanceMapper;
    }

    public T map(Cursor cursor) {
      return creator.create(
          balanceMapper.map(cursor, cursor.getColumnIndex(BALANCE))
      );
    }

    public interface Creator<R extends UserModel> {
      R create(User.Money balance);
    }

    public interface BalanceMapper {
      User.Money map(Cursor cursor, int columnIndex);
    }
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final BalanceMarshal balanceMarshal;

    public UserMarshal(BalanceMarshal balanceMarshal) {
      this.balanceMarshal = balanceMarshal;
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T balance(User.Money balance) {
      balanceMarshal.marshal(contentValues, BALANCE, balance);
      return (T) this;
    }

    public interface BalanceMarshal {
      void marshal(ContentValues contentValues, String columnName, User.Money balance);
    }
  }
}
