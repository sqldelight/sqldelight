package com.alecstrong.sqlite.android.model;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
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

  public void addColumn(Column<T> column) {
    columns.add(column);
    if (column.javatypeConstraint != null) {
      column.javatypeConstraint.checkIsEnum(projectPath);
    }
  }

  public void addSqlStmt(SqlStmt<T> sqlStmt) {
    sqlStmts.add(sqlStmt);
  }

  /*
   * Compiler methods
   */

  public String getPackageName() {
    return packageName;
  }

  public List<Column<T>> getColumns() {
    return columns;
  }

  public List<SqlStmt<T>> getSqlStmts() {
    return sqlStmts;
  }

  public String interfaceName() {
    return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
  }

  public String mapperName() {
    return interfaceName() + "Mapper";
  }

  public File getOutputDirectory() {
    return new File(projectPath + outputDirectory);
  }

  public TypeName interfaceType() {
    return ClassName.get(packageName, interfaceName());
  }
}
