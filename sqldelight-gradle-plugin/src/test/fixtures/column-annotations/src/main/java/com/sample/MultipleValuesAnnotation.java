package com.sample;

public @interface MultipleValuesAnnotation {
  int value1();
  String value2();
  String[] value3();
  Class<?> value4();
}