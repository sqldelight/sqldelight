package com.squareup.sqldelight;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.arch.persistence.db.SupportSQLiteQuery;
import java.util.Set;

public class SqlDelightQuery implements SupportSQLiteQuery {
  private final String sql;
  private final Set<String> tables;

  public SqlDelightQuery(String sql, Set<String> tables) {
    this.sql = sql;
    this.tables = tables;
  }

  /** A set of the tables this statement observes. */
  public final Set<String> getTables() {
    return tables;
  }

  @Override public final String getSql() {
    return sql;
  }

  @Override public void bindTo(SupportSQLiteProgram statement) {
  }
}
