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

  public static Factory<User> FACTORY = new Factory<>(new Creator() {
    @Override
    public User create(Money balance, Money balance_nullable) {
      return new User(balance, balance_nullable);
    }
  }, MONEY_ADAPTER, MONEY_ADAPTER);

  private final Money balance;
  private final Money balance_nullable;

  private User(Money balance, Money balance_nullable) {
    this.balance = balance;
    this.balance_nullable = balance_nullable;
  }

  @Override
  public Money balance() {
    return balance;
  }

  @Override
  public Money balance_nullable() {
    return balance_nullable;
  }
}
