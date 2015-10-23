package com.alecstrong.sqlite.android.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Table<T> extends SqlElement<T> {
  private final String packageName;
  private final String name;
  private final List<Column<T>> columns = new ArrayList<>();
  private final List<SqlStmt<T>> sqlStmts = new ArrayList<>();
  private final File outputDirectory;

  public Table(String packageName, String name, T originatingElement, File outputDirectory) {
    super(originatingElement);
    this.packageName = packageName;
    this.name = name;
    this.outputDirectory = outputDirectory;
  }

  public String getPackageName() {
    return packageName;
  }

  public void addColumn(Column<T> column) {
    columns.add(column);
  }

  public List<Column<T>> getColumns() {
    return columns;
  }

  public void addSqlStmt(SqlStmt<T> sqlStmt) {
    sqlStmts.add(sqlStmt);
  }

  public List<SqlStmt<T>> getSqlStmts() {
    return sqlStmts;
  }

  public String interfaceName() {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }
}
