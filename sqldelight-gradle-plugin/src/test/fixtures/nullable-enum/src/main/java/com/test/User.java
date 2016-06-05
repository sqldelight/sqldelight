package com.test;

import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.EnumColumnAdapter;

public class User implements UserModel {
  public enum Gender {
    MALE, FEMALE, OTHER
  }

  private static final ColumnAdapter<Gender> GENDER_ADAPTER =
      EnumColumnAdapter.create(Gender.class);

  private static final Mapper.Creator CREATOR = new Mapper.Creator() {
    public User create(Gender gender) {
      return new User(gender);
    }
  };

  public static final Mapper<User> MAPPER =
      new Mapper<>(CREATOR, GENDER_ADAPTER);

  public static UserModel.Marshal marshal() {
    return new UserModel.Marshal(GENDER_ADAPTER);
  }

  private final Gender gender;

  public User(Gender gender) {
    this.gender = gender;
  }

  @Override public Gender gender() {
    return gender;
  }
}