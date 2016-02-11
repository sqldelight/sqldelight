package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import com.squareup.sqldelight.ColumnAdapter;
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

    private final ColumnAdapter<User.Money> balanceMapper;

    protected Mapper(Creator<T> creator, ColumnAdapter<User.Money> balanceMapper) {
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
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Money> balanceMarshal;

    public UserMarshal(ColumnAdapter<User.Money> balanceMarshal) {
      this.balanceMarshal = balanceMarshal;
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T balance(User.Money balance) {
      balanceMarshal.marshal(contentValues, BALANCE, balance);
      return (T) this;
    }
  }
}
