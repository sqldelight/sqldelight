package com.test;

import java.lang.Override;

public class User implements UserModel {
  private static final Creator CREATOR = new Creator() {
    public User create(Boolean tall) {
      return new User(tall);
    }
  };

  public static final Factory<User> FACTORY = new Factory<>(CREATOR);

  public static UserModel.Marshal marshal() {
    return new UserModel.Marshal();
  }

  private final Boolean tall;

  public User(Boolean tall) {
    this.tall = tall;
  }

  @Override public Boolean tall() {
    return tall;
  }
}