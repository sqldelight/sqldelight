package com.alecstrong.sqlite.android.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Table<T> extends SqlElement<T> {
  private static final String outputDirectory = "build/generated-src";

  private final String packageName;
  private final String name;
  private final List<Column<T>> columns = new ArrayList<>();
  private final List<SqlStmt<T>> sqlStmts = new ArrayList<>();
  private final String projectPath;

  public Table(String packageName, String name, T originatingElement, String projectPath) {
    super(originatingElement);
    this.packageName = packageName;
    this.name = name;
    this.projectPath = projectPath;
  }

  public String getPackageName() {
    return packageName;
  }

  public void addColumn(Column<T> column) {
    columns.add(column);
    for (ColumnConstraint columnConstraint : column.columnConstraints) {
      if (columnConstraint instanceof JavatypeConstraint) {
        // Check to see if this javatype is an enum.
        ((JavatypeConstraint) columnConstraint).checkIsEnum(projectPath);
      }
    }
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
    return new File(projectPath + outputDirectory);
  }
}
