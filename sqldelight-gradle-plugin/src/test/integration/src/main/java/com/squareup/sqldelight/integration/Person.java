package com.squareup.sqldelight.integration;

import com.google.auto.value.AutoValue;

@AutoValue public abstract class Person implements PersonModel {
  public static final Factory<Person> FACTORY = new Factory<>(AutoValue_Person::new);
}
