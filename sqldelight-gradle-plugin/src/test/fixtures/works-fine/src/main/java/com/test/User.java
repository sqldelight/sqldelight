package com.test;

import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.EnumColumnAdapter;
import java.util.List;
import java.util.Map;

public class User implements UserModel {
  public enum Gender {
    MALE, FEMALE, OTHER
  }

  private static final ColumnAdapter<Gender> GENDER_ADAPTER =
      EnumColumnAdapter.create(Gender.class);
  private static final ColumnAdapter<Map<List<Integer>, Float>> MAP_ADAPTER = null;

  private static final Mapper.Creator<User> CREATOR = new Mapper.Creator<User>() {
    public User create(long id, String firstName, String middleInitial, String lastName, int age, Gender gender, Map<List<Integer>, Float> some_generic) {
      return new User(id, firstName, middleInitial, lastName, age, gender, some_generic);
    }
  };

  public static final Mapper<User> MAPPER = new Mapper<>(CREATOR, GENDER_ADAPTER, MAP_ADAPTER);

  public static UserMarshal marshal() {
    return new UserMarshal(GENDER_ADAPTER, MAP_ADAPTER);
  }

  private final long id;
  private final String firstName;
  private final String middleInitial;
  private final String lastName;
  private final int age;
  private final Gender gender;
  private final Map<List<Integer>, Float> some_generic;

  public User(long id, String firstName, String middleInitial, String lastName, int age, Gender gender, Map<List<Integer>, Float> some_generic) {
    this.id = id;
    this.firstName = firstName;
    this.middleInitial = middleInitial;
    this.lastName = lastName;
    this.age = age;
    this.gender = gender;
    this.some_generic = some_generic;
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

  @Override public Map<List<Integer>, Float> some_generic() {
    return some_generic;
  }
}
