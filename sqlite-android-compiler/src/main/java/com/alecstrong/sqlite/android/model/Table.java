package com.alecstrong.sqlite.android.model;

import java.util.ArrayList;
import java.util.List;

public class Table {
  private final String packageName;
  private final String name;
  private final List<Column> columns = new ArrayList<>();
  private final List<SqlStmt> sqlStmts = new ArrayList<>();

  public Table(String packageName, String name) {
    this.packageName = packageName;
    this.name = name;
  }

  public String getPackageName() {
    return packageName;
  }

  public void addColumn(Column column) {
    columns.add(column);
  }

  public List<Column> getColumns() {
    return columns;
  }

  public void addSqlStmt(SqlStmt sqlStmt) {
    sqlStmts.add(sqlStmt);
  }

  public List<SqlStmt> getSqlStmts() {
    return sqlStmts;
  }

  public String interfaceName() {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }
}
