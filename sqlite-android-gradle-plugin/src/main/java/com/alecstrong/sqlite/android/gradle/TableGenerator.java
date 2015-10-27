package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.JavatypeConstraint;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class TableGenerator {
  private final File outputDirectory;

  public TableGenerator(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  Table<ParserRuleContext> generateTable(SQLiteParser.ParseContext parseContext) {
    if (!parseContext.error().isEmpty()) {
      throw new IllegalStateException("Error: " + parseContext.error(0).toString());
    }

    Table<ParserRuleContext> table = null;
    List<SqlStmt<ParserRuleContext>> sqlStmts = new ArrayList<>();

    for (SQLiteParser.Sql_stmtContext sqlStatement : parseContext.sql_stmt_list(0).sql_stmt()) {
      List<Interval> omittedText = new ArrayList<>();
      if (sqlStatement.create_table_stmt() != null) {
        table = tableFor(packageName(parseContext), sqlStatement.create_table_stmt(), omittedText);
      }
      if (sqlStatement.IDENTIFIER() != null) {
        sqlStmts.add(sqlStmtFor(sqlStatement, omittedText));
      }
    }

    if (table != null) {
      sqlStmts.forEach(table::addSqlStmt);
    }
    return table;
  }

  private Table<ParserRuleContext> tableFor(String packageName,
      SQLiteParser.Create_table_stmtContext createTable, List<Interval> omittedText) {
    Table<ParserRuleContext> table =
        new Table<>(packageName, createTable.table_name().getText(), createTable, outputDirectory);
    for (SQLiteParser.Column_defContext column : createTable.column_def()) {
      table.addColumn(columnFor(column, omittedText));
    }
    return table;
  }

  private Column<ParserRuleContext> columnFor(SQLiteParser.Column_defContext column,
      List<Interval> omittedText) {
    String columnName = column.column_name().getText();
    Column.Type type = Column.Type.valueOf(column.type_name().getText());
    Column<ParserRuleContext> result = new Column<>(columnName, type, column);
    for (SQLiteParser.Column_constraintContext constraintNode : column.column_constraint()) {
      ColumnConstraint<ParserRuleContext> constraint = constraintFor(constraintNode, omittedText);
      if (constraint != null) result.columnConstraints.add(constraint);
    }
    return result;
  }

  private ColumnConstraint<ParserRuleContext> constraintFor(
      SQLiteParser.Column_constraintContext constraint, List<Interval> omittedText) {
    if (constraint.K_JAVATYPE() != null) {
      omittedText.add(Interval.of(constraint.start.getStartIndex() - 1, constraint.stop.getStopIndex() + 1));
      return new JavatypeConstraint<>(constraint.STRING_LITERAL().getText(), constraint);
    }
    return null;
  }

  private SqlStmt<ParserRuleContext> sqlStmtFor(SQLiteParser.Sql_stmtContext sqlStatementParent,
      List<Interval> omittedText) {
    ParserRuleContext sqlStatement =
        (ParserRuleContext) sqlStatementParent.getChild(sqlStatementParent.getChildCount() - 1);
    List<Integer> offsets = Stream.concat(
        omittedText.stream().flatMap(interval -> Stream.of(interval.a, interval.b)),
        Stream.of(sqlStatement.start.getStartIndex(), sqlStatement.stop.getStopIndex() + 1))
        .sorted()
        .collect(Collectors.toList());
    List<Interval> intervals = new ArrayList<>();
    for (int i = 0; i < offsets.size(); i += 2) {
      intervals.add(Interval.of(offsets.get(i), offsets.get(i + 1)));
    }
    return new SqlStmt<>(sqlStatementParent.IDENTIFIER().getText(),
        getFullText(sqlStatement, intervals),
        sqlStatementParent);
  }

  private String packageName(SQLiteParser.ParseContext parseContext) {
    return Joiner.on('.')
        .join(parseContext.package_stmt(0).name().stream().map(RuleContext::getText).iterator());
  }

  private String getFullText(ParserRuleContext context, List<Interval> intervals) {
    if (context.start == null
        || context.stop == null
        || context.start.getStartIndex() < 0
        || context.stop.getStopIndex() < 0) {
      return context.getText(); // Fallback
    }

    StringBuilder stringBuilder = new StringBuilder();
    for (Interval interval : intervals) {
      stringBuilder.append(context.start.getInputStream().getText(interval));
    }
    return stringBuilder.toString();
  }
}
