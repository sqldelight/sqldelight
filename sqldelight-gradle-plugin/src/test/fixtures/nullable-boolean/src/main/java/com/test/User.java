package com.test;

import java.lang.Override;

public class User implements UserModel {
  private static final Mapper.Creator CREATOR = new Mapper.Creator() {
    public User create(Boolean tall) {
      return new User(tall);
    }
  };

  public static final Mapper<User> MAPPER = new Mapper<>(CREATOR);

  public static UserMarshal marshal() {
    return new UserMarshal();
  }

  private final Boolean tall;

  public User(Boolean tall) {
    this.tall = tall;
  }

  @Override public Boolean tall() {
    return tall;
  }
}