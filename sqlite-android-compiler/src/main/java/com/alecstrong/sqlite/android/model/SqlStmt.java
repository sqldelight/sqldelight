package com.alecstrong.sqlite.android.model;

public class SqlStmt {
  public final String identifier;
  public final String stmt;

  public SqlStmt(String identifier, String stmt) {
    this.identifier = identifier;
    this.stmt = stmt;
  }
}
