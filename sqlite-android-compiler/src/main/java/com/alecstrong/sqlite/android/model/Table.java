package com.alecstrong.sqlite.android.model;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;

public final class Table<T> extends SqlElement<T> {
  private final String packageName;
  private final String name;
  private final String interfaceName;
  private final List<Column<T>> columns = new ArrayList<Column<T>>();
  private final boolean isKeyValue;

  public Table(String packageName, String interfaceName, String name, T originatingElement,
      boolean isKeyValue) {
    super(originatingElement);
    this.packageName = packageName;
    this.interfaceName = interfaceName;
    this.name = name;
    this.isKeyValue = isKeyValue;
  }

  public void addColumn(Column<T> column) {
    columns.add(column);
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

  private String modelName() {
    return interfaceName;
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

  public TypeName interfaceType() {
    return ClassName.get(packageName, interfaceName());
  }

  public boolean isKeyValue() {
    return isKeyValue;
  }
}
