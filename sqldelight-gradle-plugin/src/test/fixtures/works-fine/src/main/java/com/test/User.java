package com.test;

import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.EnumColumnAdapter;
import java.util.List;
import java.util.Map;

public class User implements UserModel {
  public enum Gender {
    MALE, FEMALE, OTHER
  }

  private static final ColumnAdapter<Gender, String> GENDER_ADAPTER =
      EnumColumnAdapter.create(Gender.class);
  private static final ColumnAdapter<Map<List<Integer>, Float>, byte[]> MAP_ADAPTER = null;
  private static final ColumnAdapter<List<Map<List<List<Integer>>, List<Integer>>>, byte[]> LIST_ADAPTER = null;
  private static final ColumnAdapter<User, byte[]> USER_ADAPTER = null;
  private static final ColumnAdapter<List<List<List<List<String>>>>, byte[]> OTHER_LIST_ADAPTER = null;

  private static final Creator<User> CREATOR = new Creator<User>() {
    public User create(long id, String firstName, String middleInitial, String lastName, int age,
        Gender gender, Map<List<Integer>, Float> some_generic,
        List<Map<List<List<Integer>>, List<Integer>>> some_list, Gender gender2, User full_user,
        List<List<List<List<String>>>> such_list) {
      return new User(id, firstName, middleInitial, lastName, age, gender, some_generic,
          some_list, gender2, full_user, such_list);
    }
  };

  public static final Factory<User> FACTORY = new Factory<>(CREATOR, GENDER_ADAPTER, MAP_ADAPTER,
      LIST_ADAPTER, GENDER_ADAPTER, USER_ADAPTER, OTHER_LIST_ADAPTER);

  private final long id;
  private final String firstName;
  private final String middleInitial;
  private final String lastName;
  private final int age;
  private final Gender gender;
  private final Map<List<Integer>, Float> some_generic;
  private final List<Map<List<List<Integer>>, List<Integer>>> some_list;
  private final Gender gender2;
  private final User full_user;
  private final List<List<List<List<String>>>> such_list;

  public User(long id, String firstName, String middleInitial, String lastName, int age,
      Gender gender, Map<List<Integer>, Float> some_generic,
      List<Map<List<List<Integer>>, List<Integer>>> some_list, Gender gender2, User full_user,
      List<List<List<List<String>>>> such_list) {
    this.id = id;
    this.firstName = firstName;
    this.middleInitial = middleInitial;
    this.lastName = lastName;
    this.age = age;
    this.gender = gender;
    this.some_generic = some_generic;
    this.some_list = some_list;
    this.gender2 = gender2;
    this.full_user = full_user;
    this.such_list = such_list;
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

  @Override public List<Map<List<List<Integer>>, List<Integer>>> some_list() {
    return some_list;
  }

  @Override public Gender gender2() {
    return gender2;
  }

  @Override public User full_user() {
    return full_user;
  }

  @Override public List<List<List<List<String>>>> such_list() {
    return such_list;
  }
}
