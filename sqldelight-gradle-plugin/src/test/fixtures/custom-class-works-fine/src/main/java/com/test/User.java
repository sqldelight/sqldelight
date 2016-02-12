package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.Override;
import com.squareup.sqldelight.ColumnAdapter;

public class User implements UserModel {
  public static class Money {
    final int dollars;
    final int cents;

    Money(int dollars, int cents) {
      this.dollars = dollars;
      this.cents = cents;
    }
  }

  public static ColumnAdapter<Money> MONEY_ADAPTER = new ColumnAdapter<Money>() {
    @Override
    public Money map(Cursor cursor, int columnIndex) {
      String[] money = cursor.getString(columnIndex).split(".");
      return new Money(Integer.parseInt(money[0]), Integer.parseInt(money[1]));
    }

    @Override
    public void marshal(ContentValues contentValues, String columnName, Money balance) {
      contentValues.put(columnName, balance.dollars + "." + balance.cents);
    }
  };

  public static Mapper<User> MAPPER = new Mapper<>(new Mapper.Creator() {
    @Override
    public User create(Money balance) {
      return new User(balance);
    }
  }, MONEY_ADAPTER);

  public static UserMarshal marshal() {
    return new UserMarshal(MONEY_ADAPTER);
  }

  private final Money balance;

  private User(Money balance) {
    this.balance = balance;
  }

  @Override
  public Money balance() {
    return balance;
  }
}
