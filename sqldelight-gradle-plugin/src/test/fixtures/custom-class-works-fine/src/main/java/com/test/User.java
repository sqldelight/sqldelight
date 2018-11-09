package com.test;

import android.database.Cursor;
import java.lang.Override;
import com.squareup.sqldelight.prerelease.ColumnAdapter;

public class User implements UserModel {
  public static class Money {
    final int dollars;
    final int cents;

    Money(int dollars, int cents) {
      this.dollars = dollars;
      this.cents = cents;
    }
  }

  public static ColumnAdapter<Money, String> MONEY_ADAPTER = new ColumnAdapter<Money, String>() {
    @Override
    public Money decode(String databaseValue) {
      String[] money = databaseValue.split(".");
      return new Money(Integer.parseInt(money[0]), Integer.parseInt(money[1]));
    }

    @Override
    public String encode(Money balance) {
      return balance.dollars + "." + balance.cents;
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
