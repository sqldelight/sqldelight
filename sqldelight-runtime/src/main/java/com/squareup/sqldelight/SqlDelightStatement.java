package com.squareup.sqldelight;

public class SqlDelightStatement {
  public final String statement;
  public final String[] args;

  public SqlDelightStatement(String statement, String[] args) {
    this.statement = statement;
    this.args = args;
  }
}
