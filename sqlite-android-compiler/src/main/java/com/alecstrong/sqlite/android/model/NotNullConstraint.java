package com.alecstrong.sqlite.android.model;

public final class NotNullConstraint<T> extends ColumnConstraint<T> {
  public NotNullConstraint(T originatingElement) {
    super(originatingElement);
  }
}
