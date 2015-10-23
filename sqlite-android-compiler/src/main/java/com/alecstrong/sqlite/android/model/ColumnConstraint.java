package com.alecstrong.sqlite.android.model;

public abstract class ColumnConstraint<T> extends SqlElement<T> {
  public ColumnConstraint(T originatingElement) {
    super(originatingElement);
  }
}
