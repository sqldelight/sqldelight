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
    public User create(long id, String firstName, String middleInitial, String lastName, int age, Gender gender) {
      return new User(id, firstName, middleInitial, lastName, age, gender);
    }
  };

  public static final Mapper<User> MAPPER = new Mapper<>(CREATOR, GENDER_ADAPTER);

  public static UserMarshal marshal() {
    return new UserMarshal(GENDER_ADAPTER);
  }

  private final long id;
  private final String firstName;
  private final String middleInitial;
  private final String lastName;
  private final int age;
  private final Gender gender;

  public User(long id, String firstName, String middleInitial, String lastName, int age, Gender gender) {
    this.id = id;
    this.firstName = firstName;
    this.middleInitial = middleInitial;
    this.lastName = lastName;
    this.age = age;
    this.gender = gender;
  }

  @Override public long id() {
    return id;
  }

  @Override public String first_name() {
    return firstName;
  }

  @Override public String middle_initial() {
    return middleInitial;
  }

  @Override public String last_name() {
    return lastName;
  }

  @Override public int age() {
    return age;
  }

  @Override public Gender gender() {
    return gender;
  }
}
