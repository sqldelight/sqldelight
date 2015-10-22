package com.alecstrong.sqlite.android.model;

public class Column {
  public enum Type {
    INTEGER, REAL, TEXT, BLOB
  }

  public final String name;
  public final Type type;

  public Column(String name, Type type) {
    this.name = name;
    this.type = type;
  }
}
