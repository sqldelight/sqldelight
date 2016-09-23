package com.squareup.sqldelight;

import java.util.Set;

public class SqlDelightStatement {
  public final String statement;
  public final String[] args;
  /** A set of the tables this statement observes. */
  public final Set<String> tables;

  public SqlDelightStatement(String statement, String[] args, Set<String> tables) {
    this.statement = statement;
    this.args = args;
    this.tables = tables;
  }
}
