package com.alecstrong.sqlite.android.model;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.alecstrong.sqlite.android.SqliteCompiler.getFileExtension;

public class Table<T> extends SqlElement<T> {
  public static final String outputDirectory = "generated/source/sqlite";

  private final String packageName;
  private final String name;
  private final String fileName;
  private final List<Column<T>> columns = new ArrayList<Column<T>>();
  private final List<SqlStmt<T>> sqlStmts = new ArrayList<SqlStmt<T>>();
  private final String projectPath;

  public Table(String packageName, String fileName, String name, T originatingElement, String projectPath) {
    super(originatingElement);
    this.packageName = packageName;
    this.fileName = fileName.endsWith(getFileExtension()) //
        ? fileName.substring(0, fileName.length() - (getFileExtension().length() + 1)) //
        : fileName;
    this.name = name;
    this.projectPath = projectPath;
  }

  public void addColumn(Column<T> column) {
    columns.add(column);
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

  private String modelName() {
    return fileName;
  }

  public String sqlTableName() {
    return name;
  }

  public String interfaceName() {
    return SqliteCompiler.interfaceName(modelName());
  }

  public String mapperName() {
    return modelName() + "Mapper";
  }

  public String marshalName() {
    return modelName() + "Marshal";
  }

  public File getOutputDirectory() {
    return new File(projectPath + "build/" + outputDirectory);
  }

  public String fileName() {
    return interfaceName() + ".java";
  }

  public File getFileDirectory() {
    return new File(getOutputDirectory(), Joiner.on('/').join(packageName.split("\\.")));
  }

  public TypeName interfaceType() {
    return ClassName.get(packageName, interfaceName());
  }
}
