package com.alecstrong.sqlite.android.model;

public class NotNullConstraint<T> extends ColumnConstraint<T> {
  public NotNullConstraint(T originatingElement) {
    super(originatingElement);
  }
}
