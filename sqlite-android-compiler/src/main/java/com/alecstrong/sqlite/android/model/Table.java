package com.alecstrong.sqlite.android.model;

import java.util.ArrayList;
import java.util.List;

public class Table {
  public final String packageName;
  public final String name;
  public final List<Column> columns = new ArrayList<>();

  public Table(String packageName, String name) {
    this.packageName = packageName;
    this.name = name;
  }

  public void addColumn(Column column) {
    columns.add(column);
  }
}
