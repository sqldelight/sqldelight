package com.alecstrong.sqlite.android.model;

import com.google.common.base.CaseFormat;

public class SqlStmt<T> extends SqlElement<T> {
  private final String identifier;

  public final String stmt;

  public SqlStmt(String identifier, String stmt, T originatingElement) {
    super(originatingElement);
    this.identifier = identifier;
    this.stmt = stmt;
  }

  public String getIdentifier() {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, identifier);
  }
}
