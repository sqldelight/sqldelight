package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement;
import com.alecstrong.sqlite.android.model.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TableGenerator<
    OriginatingType,
    SqliteStatementType extends OriginatingType,
    TableType extends OriginatingType,
    ColumnType extends OriginatingType,
    ConstraintType extends OriginatingType> {
  private static final String CREATE_KEY_VALUE_TABLE = "\n"
      + "CREATE TABLE %s (\n"
      + "  key TEXT NOT NULL PRIMARY KEY,\n"
      + "  value BLOB\n"
      + ");";

  private final Table<OriginatingType> table;
  private final List<SqlStmt<OriginatingType>> sqliteStatements;

  public TableGenerator(OriginatingType rootElement, String packageName, String fileName,
      String projectPath) {
    Table<OriginatingType> table = null;
    sqliteStatements = new ArrayList<SqlStmt<OriginatingType>>();

    for (SqliteStatementType sqlStatementElement : sqlStatementElements(rootElement)) {
      List<Replacement> replacements = new ArrayList<Replacement>();
      TableType tableElement = tableElement(sqlStatementElement);
      if (tableElement != null) {
        table = tableFor(tableElement, packageName, fileName, projectPath, replacements);
        if (table.isKeyValue()) {
          if (identifier(sqlStatementElement) != null) {
            sqliteStatements.add(new SqlStmt<OriginatingType>(identifier(sqlStatementElement),
                String.format(CREATE_KEY_VALUE_TABLE, tableName(tableElement)), 0,
                Collections.<Replacement>emptyList(), tableElement));
          }
          continue;
        }
      }

      if (identifier(sqlStatementElement) != null) {
        sqliteStatements.add(sqliteStatementFor(sqlStatementElement, replacements));
      }
    }

    this.table = table;
  }

  protected abstract Iterable<SqliteStatementType> sqlStatementElements(
      OriginatingType originatingElement);

  protected abstract TableType tableElement(SqliteStatementType sqlStatementElement);

  protected abstract String identifier(SqliteStatementType sqlStatementElement);

  protected abstract Iterable<ColumnType> columnElements(TableType tableElement);

  protected abstract String tableName(TableType tableElement);

  protected abstract boolean isKeyValue(TableType tableElement);

  protected abstract String columnName(ColumnType columnElement);

  protected abstract String classLiteral(ColumnType columnElement);

  protected abstract String typeName(ColumnType columnElement);

  protected abstract Replacement replacementFor(ColumnType columnElement, Column.Type type);

  protected abstract Iterable<ConstraintType> constraintElements(ColumnType columnElement);

  protected abstract ColumnConstraint<OriginatingType> constraintFor(
      ConstraintType constraintElement, List<Replacement> replacements);

  protected abstract String text(SqliteStatementType sqliteStatementElement);

  protected abstract int startOffset(SqliteStatementType sqliteStatementElement);

  private Table<OriginatingType> tableFor(TableType tableElement, String packageName,
      String fileName, String projectPath, List<Replacement> replacements) {
    Table<OriginatingType> table =
        new Table<OriginatingType>(packageName, fileName, tableName(tableElement), tableElement,
            projectPath, isKeyValue(tableElement));

    for (ColumnType columnElement : columnElements(tableElement)) {
      table.addColumn(columnFor(columnElement, replacements));
    }
    return table;
  }

  private Column<OriginatingType> columnFor(ColumnType columnElement,
      List<Replacement> replacements) {
    String columnName = columnName(columnElement);
    Column<OriginatingType> result;
    Column.Type type = Column.Type.valueOf(typeName(columnElement));
    replacements.add(replacementFor(columnElement, type));
    if (classLiteral(columnElement) != null) {
      result =
          new Column<OriginatingType>(columnName, type, classLiteral(columnElement), columnElement);
    } else {
      result = new Column<OriginatingType>(columnName, type, columnElement);
    }

    for (ConstraintType constraintElement : constraintElements(columnElement)) {
      ColumnConstraint<OriginatingType> constraint = constraintFor(constraintElement, replacements);
      if (constraint != null) result.addConstraint(constraint);
    }
    return result;
  }

  private SqlStmt<OriginatingType> sqliteStatementFor(SqliteStatementType sqliteStatementElement,
      List<Replacement> replacements) {
    return new SqlStmt<OriginatingType>(
        identifier(sqliteStatementElement),
        text(sqliteStatementElement),
        startOffset(sqliteStatementElement),
        replacements,
        sqliteStatementElement);
  }

  public Table<OriginatingType> table() {
    return table;
  }

  List<SqlStmt<OriginatingType>> sqliteStatements() {
    return sqliteStatements;
  }
}
