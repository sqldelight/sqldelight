package com.test;

import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.EnumColumnAdapter;
import java.util.List;
import java.util.Map;

public class User implements UserModel {
  private static final Creator<User> CREATOR = new Creator<User>() {
    public User create(long id, int age, String gender) {
      return new User(id, age, gender);
    }
  };

  public static final Factory<User> FACTORY = new Factory<>(CREATOR);

  private final long id;
  private final int age;
  private final String gender;

  public User(long id, int age, String gender) {
    this.id = id;
    this.age = age;
    this.gender = gender;
  }

  @Override public long id() {
    return id;
  }

  @Override public int age() {
    return age;
  }

  @Override public String gender() {
    return gender;
  }
}
