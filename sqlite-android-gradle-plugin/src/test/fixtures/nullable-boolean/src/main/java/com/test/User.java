package com.test;

import java.lang.Override;

public class User implements UserModel {
  private static final UserMapper.Creator CREATOR = new UserMapper.Creator() {
    public User create(Boolean tall) {
      return new User(tall);
    }
  };

  public static final UserMapper<User> MAPPER = new UserMapper(CREATOR);

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