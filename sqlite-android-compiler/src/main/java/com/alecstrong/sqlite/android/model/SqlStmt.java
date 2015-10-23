package com.alecstrong.sqlite.android.model;

public class SqlStmt<T> extends SqlElement<T> {
  public final String identifier;
  public final String stmt;

  public SqlStmt(String identifier, String stmt, T originatingElement) {
    super(originatingElement);
    this.identifier = identifier;
    this.stmt = stmt;
  }
}
