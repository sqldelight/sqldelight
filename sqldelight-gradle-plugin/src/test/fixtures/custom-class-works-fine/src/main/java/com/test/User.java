package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.Override;

public class User implements UserModel {
  public static class Money {
    final int dollars;
    final int cents;

    Money(int dollars, int cents) {
      this.dollars = dollars;
      this.cents = cents;
    }
  }

  public static Mapper<User> MAPPER = new Mapper<>(new Mapper.Creator() {
    @Override
    public User create(Money balance) {
      return new User(balance);
    }
  }, new Mapper.BalanceMapper() {
    @Override
    public Money map(Cursor cursor, int columnIndex) {
      String[] money = cursor.getString(columnIndex).split(".");
      return new Money(Integer.parseInt(money[0]), Integer.parseInt(money[1]));
    }
  });

  public static UserMarshal marshal() {
    return new UserMarshal(new UserMarshal.BalanceMarshal() {
      @Override
      public void marshal(ContentValues contentValues, String columnName, Money balance) {
        contentValues.put(columnName, balance.dollars + "." + balance.cents);
      }
    });
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
