package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.SqlElement;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alecstrong.sqlite.android.SqliteCompiler.getFileExtension;

public abstract class TableGenerator<
    OriginatingType,
    SqliteStatementType extends OriginatingType,
    TableType extends OriginatingType,
    ColumnType extends OriginatingType,
    ConstraintType extends OriginatingType> extends SqlElement<OriginatingType> {
  private static final String CREATE_KEY_VALUE_TABLE = "\n"
      + "CREATE TABLE %s (\n"
      + "  " + SqliteCompiler.KEY_VALUE_KEY_COLUMN + " TEXT NOT NULL PRIMARY KEY,\n"
      + "  " + SqliteCompiler.KEY_VALUE_VALUE_COLUMN + " BLOB\n"
      + ");";
  private static final String CREATE_TABLE_IDENTIFIER = "createTable";

  public static final String outputDirectory = "generated/source/sqlite";

  private final Table<OriginatingType> table;
  private final List<SqlStmt<OriginatingType>> sqliteStatements;
  private final String interfaceName;
  private final String packageName;
  private final String projectPath;

  public TableGenerator(OriginatingType rootElement, String packageName, String sqliteFileName,
      String projectPath) {
    super(rootElement);
    this.packageName = packageName;
    this.projectPath = projectPath;
    this.interfaceName = sqliteFileName.endsWith(getFileExtension()) //
        ? sqliteFileName.substring(0, sqliteFileName.length() - (getFileExtension().length() + 1))
        : sqliteFileName;
    this.sqliteStatements = new ArrayList<SqlStmt<OriginatingType>>();

    Table<OriginatingType> table = null;
    try {
      TableType tableElement = tableElement(rootElement);
      if (tableElement != null) {
        List<Replacement> replacements = new ArrayList<Replacement>();
        table = tableFor(tableElement, packageName, interfaceName, replacements);
        if (table.isKeyValue()) {
          sqliteStatements.add(new SqlStmt<OriginatingType>(CREATE_TABLE_IDENTIFIER,
              String.format(CREATE_KEY_VALUE_TABLE, tableName(tableElement)), 0,
              Collections.<Replacement>emptyList(), tableElement));
        } else {
          sqliteStatements.add(new SqlStmt<OriginatingType>( //
              CREATE_TABLE_IDENTIFIER, text(tableElement), startOffset(tableElement), replacements,
              tableElement));
        }
      }

      for (SqliteStatementType sqlStatementElement : sqlStatementElements(rootElement)) {
        List<Replacement> replacements = new ArrayList<Replacement>();
        if (identifier(sqlStatementElement) != null) {
          sqliteStatements.add(sqliteStatementFor(sqlStatementElement, replacements));
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      // Do nothing, just an easy way to catch a lot of situations where sql is incomplete.
      table = null;
    }
    this.table = table;
  }

  protected abstract Iterable<SqliteStatementType> sqlStatementElements(
      OriginatingType originatingElement);

  protected abstract TableType tableElement(OriginatingType sqlStatementElement);

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

  protected abstract String text(OriginatingType sqliteStatementElement);

  protected abstract int startOffset(OriginatingType sqliteStatementElement);

  private Table<OriginatingType> tableFor(TableType tableElement, String packageName,
      String fileName, List<Replacement> replacements) {
    Table<OriginatingType> table =
        new Table<OriginatingType>(packageName, fileName, tableName(tableElement), tableElement,
            isKeyValue(tableElement));

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

  public File getOutputDirectory() {
    return new File(projectPath + "build/" + outputDirectory);
  }

  public String fileName() {
    return interfaceName() + ".java";
  }

  /**
   * @return the package directory structure for the generated file (format: 'com/sample/package')
   */
  public String packageDirectory() {
    return Joiner.on('/').join(packageName().split("\\."));
  }

  String packageName() {
    return packageName;
  }

  File getFileDirectory() {
    return new File(getOutputDirectory(), packageDirectory());
  }

  String interfaceName() {
    return interfaceName + "Model";
  }

  List<SqlStmt<OriginatingType>> sqliteStatements() {
    return sqliteStatements;
  }
}
