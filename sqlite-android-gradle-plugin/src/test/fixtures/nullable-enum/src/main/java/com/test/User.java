package com.test;

import java.lang.Override;

public class User implements UserModel {
  public enum Gender {
    MALE, FEMALE, OTHER
  }

  private static final UserMapper.Creator CREATOR = new UserMapper.Creator() {
    public User create(Gender gender) {
      return new User(gender);
    }
  };

  public static final UserMapper<User> MAPPER = new UserMapper(CREATOR);

  public static UserMarshal marshal() {
    return new UserMarshal();
  }

  private final Gender gender;

  public User(Gender gender) {
    this.gender = gender;
  }

  @Override public Gender gender() {
    return gender;
  }
}