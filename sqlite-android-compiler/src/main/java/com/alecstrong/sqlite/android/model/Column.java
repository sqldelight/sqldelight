package com.alecstrong.sqlite.android.model;

public class Column<T> extends SqlElement<T> {
  public enum Type {
    INTEGER, REAL, TEXT, BLOB
  }

  public final String name;
  public final Type type;

  public Column(String name, Type type, T originatingElement) {
    super(originatingElement);
    this.name = name;
    this.type = type;
  }
}
